package nl.ulso.markdown_curator;

import com.google.inject.Inject;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.lang.Thread.currentThread;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.writeString;
import static java.util.Comparator.comparingInt;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static java.util.stream.Collectors.groupingBy;
import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.vaultRefreshed;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Markdown curator on top of a {@link Vault} and custom {@link DataModel}s and {@link Query}s..
 * <p/>
 * Whenever a change in the underlying vault is detected, the data models are refreshed, after
 * which all queries are collected from all documents in the vault, executed and compared to the
 * existing query results as stored inside the documents. If a query result has changed, the
 * document that contains it is rewritten to disk, with the old query result replaced.
 * <p/>
 * This curator applies parallelism by first refreshing all data models in parallel, waiting for
 * that to complete, and then running all queries in parallel. Once all queries have completed the
 * documents whose contents have changed are written back to disk, sequentially. In practice
 * there are many queries embedded in documents, while the number of documents that need updating
 * is limited.
 * <p/>
 * I haven't taken the time to prove that this parallel implementation is faster than a sequential
 * one. I applied parallelism simply because I don't want the 10 cores of the M1 Pro processor in
 * my MacBook Pro go to waste. And because it was fun to do.
 */
public class Curator
        implements VaultChangedCallback
{
    private static final Logger LOGGER = getLogger(Curator.class);
    private static final long COOL_OFF_PERIOD_IN_MILLISECONDS = 500;

    private final Vault vault;
    private final ExecutorService executor;
    private final QueryCatalog queryCatalog;
    private final Set<DataModel> dataModels;
    private final DocumentPathResolver documentPathResolver;

    @Inject
    public Curator(
            Vault vault, DocumentPathResolver documentPathResolver, QueryCatalog queryCatalog,
            Set<DataModel> dataModels)
    {
        this.vault = vault;
        this.documentPathResolver = documentPathResolver;
        this.queryCatalog = queryCatalog;
        this.dataModels = dataModels;
        this.executor = createExecutor();
    }

    private static ExecutorService createExecutor()
    {
        try
        {
            LOGGER.debug("JDK 19 PREVIEW!: Creating virtual thread executor for concurrent tasks.");
            return newVirtualThreadPerTaskExecutor();
        }
        catch (UnsupportedOperationException e)
        {
            LOGGER.debug("Creating fixed thread pool for concurrent tasks.");
            return newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                    new CuratorThreadFactory());
        }
    }

    public void runOnce()
    {
        LOGGER.info("Running this curator once");
        vaultChanged(vaultRefreshed());
    }

    public void run()
    {
        refreshAllDataModels(vaultRefreshed());
        vault.setVaultChangedCallback(this);
        vault.watchForChanges();
    }

    @Override
    public final void vaultChanged(VaultChangedEvent event)
    {
        refreshAllDataModels(event);
        var changeset = runAllQueries().entrySet().stream()
                .sorted(comparingInt(e -> e.getKey().resultStartIndex()))
                .collect(groupingBy(e -> e.getKey().document()));
        if (!changeset.isEmpty())
        {
            coolOffToPreventConflictsInObsidian();
            changeset.forEach(this::writeDocument);
        }
    }

    private void refreshAllDataModels(VaultChangedEvent event)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Refreshing {} data model(s)", dataModels.size());
        }
        runInParallel(dataModels, model ->
        {
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Refreshing data model: {}", model.getClass().getSimpleName());
            }
            model.vaultChanged(event);
        });
    }

    /**
     * Runs all queries in the vault and collects the queries whose outputs have changed compared
     * to what's in memory right now.
     */
    Map<QueryBlock, String> runAllQueries()
    {
        Map<QueryBlock, String> writeQueue = new ConcurrentHashMap<>();
        Collection<QueryBlock> queryBlocks = vault.findAllQueryBlocks();
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Running {} queries", queryBlocks.size());
        }
        runInParallel(queryBlocks, queryBlock ->
        {
            var query = queryCatalog.query(queryBlock.queryName());
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Running query '{}' in document: {}", query.name(),
                        queryBlock.document());
            }
            final QueryResult result;
            try
            {
                result = query.run(queryBlock);
            }
            catch (RuntimeException e)
            {
                LOGGER.warn(
                        "Ignoring output due to exception while running query '{}' in document: {}",
                        query.name(), queryBlock.document().name(), e);
                return;
            }
            if (result instanceof NoOpResult)
            {
                LOGGER.trace(
                        "Ignoring output due to no-op result for query '{}'  in document: {}",
                        query.name(), queryBlock.document().name());
                return;
            }
            var output = result.toMarkdown().trim();
            if (!queryBlock.result().contentEquals(output))
            {
                LOGGER.debug("Query result change detected in document: {}",
                        queryBlock.document());
                writeQueue.put(queryBlock, output);
            }
        });
        if (LOGGER.isDebugEnabled() && writeQueue.isEmpty())
        {
            LOGGER.debug("No new query output detected. Run done.");
        }
        LOGGER.trace("Write queue item count: {}", writeQueue.size());
        return writeQueue;
    }

    void writeDocument(Document document, List<Map.Entry<QueryBlock, String>> outputs)
    {
        LOGGER.info("Rewriting document: {}", document);
        var writer = new StringWriter();
        var out = new PrintWriter(writer);
        int index = 0;
        for (Map.Entry<QueryBlock, String> entry : outputs)
        {
            var queryBlock = entry.getKey();
            printDocumentLines(out, document, index, queryBlock.resultStartIndex());
            out.println(entry.getValue());
            index = queryBlock.resultEndIndex();
        }
        printDocumentLines(out, document, index, -1);
        try
        {
            var path = documentPathResolver.resolveAbsolutePath(document);
            if (getLastModifiedTime(path).toMillis() != document.lastModified())
            {
                LOGGER.warn("Skipping rewrite because document has changed on disk: {}",
                        document);
                return;
            }
            writeString(path, writer.toString());
        }
        catch (IOException e)
        {
            LOGGER.warn("Couldn't write document: {}", document);
        }
    }

    private void printDocumentLines(PrintWriter out, Document document, int start, int end)
    {
        var lines = document.lines();
        if (end == -1)
        {
            end = lines.size();
        }
        for (int i = start; i < end; i++)
        {
            out.println(lines.get(i));
        }
    }

    /**
     * Runs an action against each of the items in the collection in parallel and waits for all
     * actions to finish.
     *
     * @param items  Collection of items to apply an action on.
     * @param action Action to apply to each item.
     * @param <I>    Class of the item.
     */
    private <I> void runInParallel(Collection<I> items, Consumer<I> action)
    {
        var latch = new CountDownLatch(items.size());
        for (I item : items)
        {
            executor.submit(() ->
            {
                try
                {
                    action.accept(item);
                }
                catch (RuntimeException e)
                {
                    LOGGER.error("Job failed to execute.", e);
                }
                finally
                {
                    latch.countDown();
                }
            });
        }
        try
        {
            latch.await();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new CuratorException(e);
        }
    }

    /*
     * If Obsidian does a massive rewrite - e.g. renaming a file or header with many links to it,
     * resulting in all these references to be updated - sometimes the curator kicks in before
     * Obsidian does, resulting in Obsidian updating a file incorrectly, in the wrong place. To
     * prevent the nasty effects of this race condition from happening we give the curator some
     * time to cool off. The curator will find that files have changed in the meantime, and will
     * not write updates. Instead, it will reprocess the files automatically.
     */
    private static void coolOffToPreventConflictsInObsidian()
    {
        try
        {
            TimeUnit.MILLISECONDS.sleep(COOL_OFF_PERIOD_IN_MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static class CuratorThreadFactory
            implements ThreadFactory
    {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable)
        {
            var name = currentThread().getName() + "-Curator-" + threadNumber.getAndIncrement();
            return new Thread(runnable, name);
        }
    }
}

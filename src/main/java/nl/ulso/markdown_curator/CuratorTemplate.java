package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.query.builtin.*;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.lang.Thread.currentThread;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.writeString;
import static java.util.Comparator.comparingInt;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.groupingBy;
import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.vaultRefreshed;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Base class for {@link Curator}s on top of a {@link FileSystemVault} and custom data models
 * and queries.
 * <p/>
 * Whenever a change in the underlying vault is detected, the data models are refreshed, after
 * which all queries are collected from all documents in the vault, executed and compared to the
 * existing query results as stored inside the documents. If a query result has changed, the
 * document that contains it is rewritten to disk, with the old query result replaced.
 * <p/>
 * This curator applies parallelism by first refreshing all data models in parallel, waiting for
 * that to complete, and then running all queries in parallel. Once all queries have completed the
 * documents whose contents have changed are written back to disk,  sequentially. In practice
 * there are many queries embedded in documents, while the number of documents that need updating
 * is limited.
 * <p/>
 * I haven't taken the time to prove that this parallel implementation is faster than a sequential
 * one. I  applied parallelism simply because I don't want the 10 cores of the M1 Pro processor in
 * my MacBook Pro go to waste. And because it was fun to do.
 */
public abstract class CuratorTemplate
        implements Curator, VaultChangedCallback
{
    private static final Logger LOGGER = getLogger(CuratorTemplate.class);

    private final FileSystemVault vault;
    private final DataModelMap dataModels;
    private final QueryCatalog queryCatalog;
    private final ExecutorService executor;

    protected CuratorTemplate()
    {
        try
        {
            this.vault = createVault();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Couldn't create vault", e);
        }
        this.queryCatalog = new InMemoryQueryCatalog();
        this.dataModels = new DataModelMap(createDataModelMap(vault));
        registerDefaultQueries(queryCatalog, vault, dataModels);
        registerQueries(queryCatalog, vault, dataModels);
        this.executor = newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
                new CuratorThreadFactory());
    }

    protected final FileSystemVault createVaultForPathInUserHome(String... path)
            throws IOException
    {
        return new FileSystemVault(Path.of(java.lang.System.getProperty("user.home"), path));
    }

    protected abstract FileSystemVault createVault()
            throws IOException;

    private Map<Class<? extends DataModel>, DataModel> createDataModelMap(FileSystemVault vault)
    {
        Map<Class<? extends DataModel>, DataModel> models = new HashMap<>();
        createDefaultDataModels(vault).forEach(model -> models.put(model.getClass(), model));
        createDataModels(vault).forEach(model -> models.put(model.getClass(), model));
        return models;
    }

    private Set<DataModel> createDefaultDataModels(Vault vault)
    {
        return Set.of(new LinksModel(vault));
    }

    protected abstract Set<DataModel> createDataModels(Vault vault);

    private void registerDefaultQueries(
            QueryCatalog queryCatalog, Vault vault,
            DataModelMap dataModels)
    {
        queryCatalog.register(new TableOfContentsQuery());
        queryCatalog.register(new ListQuery(vault));
        queryCatalog.register(new TableQuery(vault));
        queryCatalog.register(new BacklinksQuery(dataModels.get(LinksModel.class)));
        queryCatalog.register(new DeadLinksQuery(dataModels.get(LinksModel.class)));
    }

    protected abstract void registerQueries(
            QueryCatalog catalog, Vault vault,
            DataModelMap dataModels);

    @Override
    public final void runOnce()
    {
        LOGGER.info("Running this curator once");
        vaultChanged(vaultRefreshed());
    }

    @Override
    public final void run()
    {
        refreshAllDataModels(vaultRefreshed());
        vault.setVaultChangedCallback(this);
        vault.watchForChanges();
    }

    @Override
    public final void vaultChanged(VaultChangedEvent event)
    {
        refreshAllDataModels(event);
        runAllQueries().entrySet().stream()
                .sorted(comparingInt(e -> e.getKey().resultStartIndex()))
                .collect(groupingBy(e -> e.getKey().document()))
                .forEach(this::writeDocument);
    }

    private void refreshAllDataModels(VaultChangedEvent event)
    {
        var models = dataModels.models();
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Refreshing {} data models", models.size());
        }
        runInParallel(models, model -> {
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
        runInParallel(queryBlocks, queryBlock -> {
            var query = queryCatalog.query(queryBlock.queryName());
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Running query '{}' in document: {}", query.name(),
                        queryBlock.document());
            }
            QueryResult result = null;
            try
            {
                result = query.run(queryBlock);
            }
            catch (RuntimeException e)
            {
                LOGGER.warn(
                        "Ignoring output due to exception while running query '{}' in document: {}", query.name(), queryBlock.document().name(), e);
            }
            if (result != null)
            {
                var output = result.toMarkdown().trim();
                if (!queryBlock.result().contentEquals(output))
                {
                    LOGGER.debug("Query result change detected in document: {}",
                            queryBlock.document());
                    writeQueue.put(queryBlock, output);
                }
            }
        });
        LOGGER.debug("Write queue item count: {}", writeQueue.size());
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
            var path = vault.resolveAbsolutePath(document);
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

    final FileSystemVault vault()
    {
        return vault;
    }

    final QueryCatalog queryCatalog()
    {
        return queryCatalog;
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

package nl.ulso.markdown_curator;

import jakarta.inject.Inject;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.event.DocumentChanged;
import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.lang.Thread.currentThread;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.writeString;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.groupingBy;
import static nl.ulso.hash.Hasher.hash;
import static nl.ulso.markdown_curator.DocumentRewriter.rewriteDocument;
import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.vaultRefreshed;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Markdown curator on top of a {@link Vault} and custom {@link DataModel}s and {@link Query}s.
 * <p/>
 * Whenever a change in the underlying vault is detected, the data models are refreshed, after
 * which all queries are collected from all documents in the vault, executed and compared to the
 * existing query results as stored inside the documents. If a query result has changed, the
 * document that contains it is rewritten to disk, with the old query result replaced.
 * <p/>
 * Queries are not executed after every detected change. Instead, the running of queries and writing
 * of documents to disk is scheduled to take place after a short delay. If during this delay new
 * changes come in the task is rescheduled. This prevents superfluous query execution and writes to
 * disk, at the cost of the user having to wait a little while after saving the last change. This
 * is especially useful when using Obsidian, which automatically write changes to disk every few
 * seconds.
 * <p/>
 * This curator runs all queries, of which there can be many, in parallel. Once all queries have
 * completed the documents whose contents have changed are written back to disk, sequentially. In
 * practice there are many queries embedded in documents, while the number of documents that need
 * updating is limited, because most queries won't have new output.
 * <p/>
 * I haven't taken the time to prove that this parallel implementation is faster than a sequential
 * one. I applied parallelism simply because I don't want the 10 cores of the M1 Pro processor in
 * my MacBook Pro go to waste. And because it was fun to do. Nice tidbit: one of my vaults has
 * 3250 embedded queries, across 2850 documents. Running those takes a little over 40 milliseconds;
 * the CPU doesn't break a sweat.
 */
public class Curator
        implements VaultChangedCallback
{
    private static final Logger LOGGER = getLogger(Curator.class);
    private static final long SCHEDULE_TIMEOUT_IN_SECONDS = 3;

    private final Vault vault;
    private final DocumentPathResolver documentPathResolver;
    private final QueryCatalog queryCatalog;
    private final Set<DataModel> dataModels;
    private final Map<String, Long> writtenDocuments;
    private final ExecutorService parallelExecutor;
    private final ScheduledExecutorService delayedExecutor;
    private ScheduledFuture<?> runTask;
    private final String curatorName;

    @Inject
    public Curator(
            Vault vault, DocumentPathResolver documentPathResolver, QueryCatalog queryCatalog,
            Set<DataModel> dataModels)
    {
        this.vault = vault;
        this.documentPathResolver = documentPathResolver;
        this.queryCatalog = queryCatalog;
        this.dataModels = dataModels;
        this.writtenDocuments = new HashMap<>();
        this.parallelExecutor = newVirtualThreadPerTaskExecutor();
        this.delayedExecutor = newScheduledThreadPool(1);
        this.runTask = null;
        this.curatorName = currentThread().getName();
    }

    public void runOnce()
    {
        LOGGER.info("Running this curator once");
        vaultChanged(vaultRefreshed());
        cancelQueryWriteRunIfPresent();
        performQueryWriteRun();
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
        if (checkSelfTriggeredUpdate(event))
        {
            return;
        }
        cancelQueryWriteRunIfPresent();
        refreshAllDataModels(event);
        scheduleQueryWriteRun();
    }

    private boolean checkSelfTriggeredUpdate(VaultChangedEvent event)
    {
        if (!(event instanceof DocumentChanged changeEvent))
        {
            return false;
        }
        var document = changeEvent.document();
        var documentName = document.name();
        var timestamp = writtenDocuments.get(documentName);
        if (timestamp == null)
        {
            return false;
        }
        writtenDocuments.remove(documentName);
        if (document.lastModified() != timestamp)
        {
            return false;
        }
        LOGGER.debug("Ignoring change on {} because this curator caused it.", documentName);
        return true;
    }

    private void cancelQueryWriteRunIfPresent()
    {
        if (runTask != null)
        {
            LOGGER.trace("Cancelling currently scheduled task");
            runTask.cancel(false);
        }
    }

    private void scheduleQueryWriteRun()
    {
        LOGGER.debug(
                "Scheduling query processing and document writing task to run in {} seconds",
                SCHEDULE_TIMEOUT_IN_SECONDS);
        runTask = delayedExecutor.schedule(this::performQueryWriteRun,
                SCHEDULE_TIMEOUT_IN_SECONDS, SECONDS);
    }

    private void performQueryWriteRun()
    {
        MDC.put("curator", curatorName);
        LOGGER.info("Running all queries and writing document updates to disk");
        var changeset = runAllQueries().stream()
                .collect(groupingBy(item -> item.queryBlock().document()));
        changeset.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(QueryOutput::isChanged))
                .forEach(entry -> writeDocument(entry.getKey(), entry.getValue()));
        LOGGER.info("Curator run done.");
    }

    /**
     * Refresh all data models for the incoming event.
     * <p/>
     * Refreshing of data models must be done sequentially, because models are able to depend on
     * each other. The order of data models *should* be okay, but I'm not sure if this is guaranteed
     * at the moment...
     *
     * @param event The event to process.
     */
    private void refreshAllDataModels(VaultChangedEvent event)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Refreshing {} data model(s)", dataModels.size());
        }
        dataModels.forEach(model -> model.vaultChanged(event));
    }

    /**
     * Runs all queries in the vault and collects the queries whose outputs have changed compared
     * to what's in memory right now.
     */
    Queue<QueryOutput> runAllQueries()
    {
        var writeQueue = new ConcurrentLinkedQueue<QueryOutput>();
        var queryBlocks = vault.findAllQueryBlocks();
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
            var output = result.toMarkdown();
            var hash = hash(output);
            var isChanged = !queryBlock.outputHash().contentEquals(hash);
            writeQueue.add(new QueryOutput(queryBlock, output, hash, isChanged));
        });
        return writeQueue;
    }

    void writeDocument(Document document, List<QueryOutput> queryOutputs)
    {
        LOGGER.info("Rewriting document: {}", document);
        var newDocumentContent = rewriteDocument(document, queryOutputs);
        try
        {
            var path = documentPathResolver.resolveAbsolutePath(document);
            if (document.lastModified() != getLastModifiedTime(path).toMillis())
            {
                LOGGER.warn("Skipping rewrite, document has changed on disk: {}", document);
                return;
            }
            writeString(path, newDocumentContent);
            writtenDocuments.put(document.name(), getLastModifiedTime(path).toMillis());
        }
        catch (IOException e)
        {
            LOGGER.warn("Couldn't write document: {}", document);
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
            parallelExecutor.submit(() ->
            {
                MDC.put("curator", curatorName);
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
}

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
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
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
    private static final long COOL_OFF_PERIOD_IN_MILLISECONDS = 100;

    private final Vault vault;
    private final DocumentPathResolver documentPathResolver;
    private final QueryCatalog queryCatalog;
    private final Set<DataModel> dataModels;
    private final Set<String> writtenDocuments;
    private final ExecutorService executor;
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
        this.writtenDocuments = new HashSet<>();
        this.executor = newVirtualThreadPerTaskExecutor();
        this.curatorName = currentThread().getName();
    }

    public void runOnce()
    {
        LOGGER.info("Running this curator once");
        vaultChanged(vaultRefreshed());
    }

    public void run()
    {
        refreshAllDataModels(vaultRefreshed());
        System.gc();
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
        refreshAllDataModels(event);
        var changeset = runAllQueries().stream()
                .collect(groupingBy(item -> item.queryBlock().document()));
        coolOffToPreventConflicts();
        changeset.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(QueryOutput::isChanged))
                .forEach(entry -> writeDocument(entry.getKey(), entry.getValue()));
        LOGGER.info("Curator run done. Going back to waiting for incoming changes.");
        System.gc();
    }

    private boolean checkSelfTriggeredUpdate(VaultChangedEvent event)
    {
        if (event instanceof DocumentChanged changeEvent)
        {
            var documentName = changeEvent.document().name();
            if (writtenDocuments.contains(documentName))
            {
                LOGGER.info("Ignoring change on {} because this curator caused it.", documentName);
                writtenDocuments.remove(documentName);
                return true;
            }
        }
        return false;
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
            var output = result.toMarkdown().trim();
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
            if (getLastModifiedTime(path).toMillis() != document.lastModified())
            {
                LOGGER.warn("Skipping rewrite, document has changed on disk: {}", document);
            }
            writeString(path, newDocumentContent);
            writtenDocuments.add(document.name());
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
            executor.submit(() ->
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

    /*
     * Sometimes the curator kicks in too fast, resulting in an editor (e.g. Obsidian) still writing
     * files from a work queue at the same time as the curator, resulting in corrupted files. It's
     * all just text, but still... To prevent the nasty effects of this race condition from
     * happening we give the curator some time to cool off. The curator will find that files have
     * changed in the meantime, and will not write updates. Instead, it will reprocess the files
     * automatically. Eventually the curator catches up to all changes anyway, so at some point all
     * changes are written to disk.
     */
    private static void coolOffToPreventConflicts()
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
}

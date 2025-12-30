package nl.ulso.markdown_curator;

import jakarta.inject.Inject;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.Dictionary;
import nl.ulso.markdown_curator.vault.event.DocumentChanged;
import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Thread.currentThread;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.writeString;
import static java.util.Collections.emptyList;
import static java.util.Collections.reverse;
import static java.util.List.copyOf;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.groupingBy;
import static nl.ulso.hash.ShortHasher.shortHashOf;
import static nl.ulso.markdown_curator.DocumentRewriter.rewriteDocument;
import static nl.ulso.markdown_curator.vault.Dictionary.emptyDictionary;
import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.vaultRefreshed;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Markdown curator on top of a {@link Vault} and custom {@link DataModel}s and {@link Query}s.
 * <p/>
 * Whenever a change in the underlying vault is detected, the data models are refreshed, after which
 * all queries are collected from all documents in the vault, executed and compared to the existing
 * query results as stored inside the documents. If a query result has changed, the document that
 * contains it is rewritten to disk, with the old query result replaced.
 * <p/>
 * Queries are not executed after every detected change. Instead, the running of queries and writing
 * of documents to disk is scheduled to take place after a short delay. If during this delay new
 * changes come in the task is rescheduled. This prevents superfluous query execution and writes to
 * disk, at the cost of the user having to wait a little while after saving the last change. This is
 * especially useful when using Obsidian, which automatically write changes to disk every few
 * seconds.
 * <p/>
 * This curator runs all queries, of which there can be many, in parallel. Once all queries have
 * completed the documents whose contents have changed are written back to disk, sequentially. In
 * practice there are many queries embedded in documents, while the number of documents that need
 * updating is limited, because most queries won't have new output.
 * <p/>
 * I haven't taken the time to prove that this parallel implementation is faster than a sequential
 * one. I applied parallelism simply because I don't want the 10 cores of the M1 Pro processor in my
 * MacBook Pro go to waste. And because it was fun to do.
 */
public class Curator
    implements VaultChangedCallback
{
    private static final Logger LOGGER = getLogger(Curator.class);
    private static final long SCHEDULE_TIMEOUT_IN_SECONDS = 3;

    private final Vault vault;
    private final DocumentPathResolver documentPathResolver;
    private final FrontMatterRewriteResolver frontMatterRewriteResolver;
    private final QueryCatalog queryCatalog;
    private final List<DataModel> dataModels;
    private final Map<String, Long> writtenDocuments;
    private final ExecutorService parallelExecutor;
    private final ScheduledExecutorService delayedExecutor;
    private ScheduledFuture<?> runTask;
    private final String curatorName;

    @Inject
    public Curator(
        Vault vault, DocumentPathResolver documentPathResolver,
        FrontMatterRewriteResolver frontMatterRewriteResolver, QueryCatalog queryCatalog,
        Set<DataModel> dataModels)
    {
        this.vault = vault;
        this.documentPathResolver = documentPathResolver;
        this.frontMatterRewriteResolver = frontMatterRewriteResolver;
        this.queryCatalog = queryCatalog;
        this.dataModels = orderDataModels(dataModels);
        this.writtenDocuments = new HashMap<>();
        this.parallelExecutor = newVirtualThreadPerTaskExecutor();
        this.delayedExecutor = newScheduledThreadPool(1);
        this.runTask = null;
        this.curatorName = currentThread().getName();
    }

    /// Orders the data models by placing data models that other models depend on first.
    ///
    /// In theory this algorithm can run into an endless loop: if there is a cyclic dependency
    /// between data models. In practice this doesn't happen because in this system all dependencies
    /// are injected in constructors by Dagger. If there is a cyclic dependency, the code fails to
    /// compile.
    static List<DataModel> orderDataModels(Set<DataModel> dataModels)
    {
        var size = dataModels.size();
        var list = new ArrayList<DataModel>(size);
        var queue = new LinkedList<>(dataModels);
        while (!queue.isEmpty())
        {
            var model = queue.pollFirst();
            var index = model.dependentModels().stream()
                .mapToInt(list::indexOf)
                .min()
                .orElse(0);
            if (index == -1)
            {
                queue.addLast(model);
            }
            else
            {
                list.add(index, model);
            }
        }
        reverse(list);
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("{} data models will be refreshed in this order: {}", list.size(),
                list.stream().map(model -> model.getClass().getSimpleName())
                    .collect(Collectors.joining(", "))
            );
        }
        return copyOf(list);
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
    public final synchronized void vaultChanged(VaultChangedEvent event)
    {
        if (checkSelfTriggeredUpdate(event))
        {
            return;
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(">".repeat(80));
        }
        cancelQueryWriteRunIfPresent();
        refreshAllDataModels(event);
        scheduleQueryWriteRun();
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("<".repeat(80));
        }
    }

    /**
     * A change is self-triggered if it was the result of the curator writing a file to disk. To
     * cancel out those changes - no query will produce different output, guaranteed - the curator
     * keeps track of the files it changed, and then compares them with the changes it detects. If
     * they match, the change was self-triggered.
     *
     * @param event Event that triggered the curator.
     * @return {@code true} if the change was self-triggered, {@code false} otherwise.
     */
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

    /**
     * Since there is an incoming change, there's no need to write changes to disk from the previous
     * processing run; a new one will be scheduled shortly.
     */
    private void cancelQueryWriteRunIfPresent()
    {
        if (runTask != null)
        {
            LOGGER.trace("Cancelling currently scheduled task");
            runTask.cancel(false);
        }
    }

    /**
     * A change was detected, which means all queries need to be executed, and changes written to
     * disk. That work is scheduled for a few seconds from now. If new changes come in in the
     * meantime, the task will be cancelled and replaced by a new one.
     */
    private void scheduleQueryWriteRun()
    {
        LOGGER.debug("Scheduling query processing and document writing task to run in {} seconds",
            SCHEDULE_TIMEOUT_IN_SECONDS
        );
        runTask = delayedExecutor.schedule(this::performQueryWriteRun, SCHEDULE_TIMEOUT_IN_SECONDS,
            SECONDS
        );
    }

    /**
     * Run all queries, collect all output, throw away all output except from the ones that changed,
     * and write the changes back to disk.
     */
    private void performQueryWriteRun()
    {
        MDC.put("curator", curatorName);
        LOGGER.debug("Running all queries and writing document updates to disk");
        var frontMatterRewrites = frontMatterRewriteResolver.resolveFrontMatterRewrites();
        var queryOutputs =
            runAllQueries().stream().collect(groupingBy(item -> item.queryBlock().document()));
        var changedDocuments = new HashSet<Document>();
        changedDocuments.addAll(frontMatterRewrites.keySet());
        changedDocuments.addAll(queryOutputs.entrySet().stream()
            .filter(entry -> entry.getValue().stream().anyMatch(QueryOutput::isChanged))
            .map(Map.Entry::getKey).collect(Collectors.toSet()));
        for (Document document : changedDocuments)
        {
            writeDocument(
                document,
                frontMatterRewrites.getOrDefault(document, emptyDictionary()),
                queryOutputs.getOrDefault(document, emptyList())
            );
        }
    }

    /**
     * Refresh all data models for the incoming event.
     * <p/>
     * Refreshing of data models must be done sequentially, because models can depend on other
     * models, and they are not necessarily protected against and built for concurrency.
     *
     * @param event The event to process.
     */
    private void refreshAllDataModels(VaultChangedEvent event)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Refreshing {} data model(s)", dataModels.size());
        }
        dataModels.forEach(model -> {
            try
            {
                model.vaultChanged(event);
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Refreshed data model {}", model.getClass().getSimpleName());
                }
            }
            catch (RuntimeException e)
            {
                LOGGER.error("Caught runtime exception while refreshing data model {}",
                    model.getClass().getSimpleName(), e
                );
            }
        });
    }

    /**
     * Run all queries in the vault and collect the queries whose outputs have changed compared to
     * what's in memory right now.
     * <p/>
     * Note that we have to collect and keep the output of all queries, even the ones that didn't
     * change. Query output is not kept in memory between runs. Documents may embed more than one
     * query, and documents are written to disks a whole. So, the outputs of all embedded queries
     * need to be available.
     * <p/>
     * The queries are executed in parallel as much as possible, because there can be thousands of
     * them.
     */
    Queue<QueryOutput> runAllQueries()
    {
        var writeQueue = new ConcurrentLinkedQueue<QueryOutput>();
        var queryBlocks = vault.findAllQueryBlocks();
        var duration = runInParallel(queryBlocks, queryBlock -> {
                var query = queryCatalog.query(queryBlock.queryName());
                if (LOGGER.isTraceEnabled())
                {
                    LOGGER.trace("Running query '{}' in document: {}", query.name(),
                        queryBlock.document()
                    );
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
                        query.name(), queryBlock.document().name(), e
                    );
                    return;
                }
                var output = result.toMarkdown();
                var hash = shortHashOf(output);
                var isChanged = !queryBlock.outputHash().contentEquals(hash);
                writeQueue.add(new QueryOutput(queryBlock, output, hash, isChanged));
            }
        );
        LOGGER.info("Executed {} queries in {} ms", queryBlocks.size(), duration);
        return writeQueue;
    }

    /**
     * Write a document to disk, but only if it hasn't changed since the start of the run.
     *
     * @param document     The document to write.
     * @param frontMatter  The new front matter for this document; can be an empty dictionary, in
     *                     which case the existing front matter is kept.
     * @param queryOutputs The fresh output of all queries on the page.
     */
    void writeDocument(Document document, Dictionary frontMatter, List<QueryOutput> queryOutputs)
    {
        LOGGER.info("Rewriting document: {}", document);
        var newDocumentContent = rewriteDocument(document, frontMatter, queryOutputs);
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
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Run an action against each of the items in the collection in parallel and wait for all
     * actions to finish.
     *
     * @param items  Collection of items to apply an action on.
     * @param action Action to apply to each item.
     * @param <I>    Class of the item.
     * @return The duration of the run in milliseconds
     */
    private <I> long runInParallel(Collection<I> items, Consumer<I> action)
    {
        var latch = new CountDownLatch(items.size());
        var startTime = System.currentTimeMillis();
        for (I item : items)
        {
            parallelExecutor.submit(() -> {
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
        return System.currentTimeMillis() - startTime;
    }
}

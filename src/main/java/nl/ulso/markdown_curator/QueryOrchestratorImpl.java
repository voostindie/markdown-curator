package nl.ulso.markdown_curator;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.query.QueryCatalog;
import nl.ulso.markdown_curator.query.QueryResult;
import nl.ulso.markdown_curator.vault.Document;
import nl.ulso.markdown_curator.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static java.util.stream.Collectors.groupingBy;
import static nl.ulso.hash.ShortHasher.shortHashOf;
import static nl.ulso.markdown_curator.vault.Dictionary.emptyDictionary;
import static org.slf4j.LoggerFactory.getLogger;

/// This orchestrator runs all queries, of which there can be many, in parallel. In practice there
/// are many queries embedded in documents, while the number of documents that need updating is
/// limited because most queries won't have new output.
@Singleton
final class QueryOrchestratorImpl
    implements QueryOrchestrator
{
    private static final Logger LOGGER = getLogger(QueryOrchestratorImpl.class);

    private final Vault vault;
    private final QueryCatalog queryCatalog;
    private final FrontMatterRewriteResolver frontMatterRewriteResolver;
    private final ExecutorService parallelExecutor;

    @Inject
    QueryOrchestratorImpl(
        Vault vault, QueryCatalog queryCatalog,
        FrontMatterRewriteResolver frontMatterRewriteResolver)
    {
        this.vault = vault;
        this.queryCatalog = queryCatalog;
        this.frontMatterRewriteResolver = frontMatterRewriteResolver;
        this.parallelExecutor = newVirtualThreadPerTaskExecutor();
    }

    /// Run all queries, collect all output, throw away all output except from the ones that changed,
    /// and write the changes back to disk.
    @Override
    public Set<DocumentUpdate> runFor(Changelog changelog)
    {
        LOGGER.debug("Running all queries");
        var frontMatterRewrites = frontMatterRewriteResolver.resolveFrontMatterRewrites();
        var queryOutputs =
            runAllQueries().stream().collect(groupingBy(item -> item.queryBlock().document()));
        var changedDocuments = new HashSet<Document>();
        changedDocuments.addAll(frontMatterRewrites.keySet());
        changedDocuments.addAll(queryOutputs.entrySet().stream()
            .filter(entry -> entry.getValue().stream().anyMatch(QueryOutput::isChanged))
            .map(Map.Entry::getKey).collect(Collectors.toSet()));
        return changedDocuments.stream()
            .map(document ->
                new DocumentUpdate(
                    document,
                    frontMatterRewrites.getOrDefault(document, emptyDictionary()),
                    queryOutputs.getOrDefault(document, emptyList())
                )
            )
            .collect(Collectors.toSet());
    }

    /// Run all queries in the vault and collect the queries whose outputs have changed compared to
    /// what's in memory right now.
    ///
    /// Note that we have to collect and keep the output of all queries, even the ones that didn't
    /// change. Query output is not kept in memory between runs. Documents may embed more than one
    /// query, and documents are written to disk as a whole. So, the outputs of all embedded queries
    /// of all documents that need an update need to be available.
    ///
    /// The queries are executed in parallel as much as possible because there can be thousands of
    /// them.
    Queue<QueryOutput> runAllQueries()
    {
        var writeQueue = new ConcurrentLinkedQueue<QueryOutput>();
        var queryBlocks = vault.findAllQueryBlocks();
        var duration = runInParallel(queryBlocks, queryBlock ->
            {
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

    /// Run an action against each of the items in the collection in parallel and wait for all
    /// actions to finish.
    ///
    /// @param items  Collection of items to apply an action on.
    /// @param action Action to apply to each item.
    /// @param <I>    Class of the item.
    /// @return The duration of the run in milliseconds
    private <I> long runInParallel(Collection<I> items, Consumer<I> action)
    {
        var latch = new CountDownLatch(items.size());
        var startTime = System.currentTimeMillis();
        var curatorName = MDC.get("curator");
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
            currentThread().interrupt();
            throw new CuratorException(e);
        }
        return System.currentTimeMillis() - startTime;
    }
}

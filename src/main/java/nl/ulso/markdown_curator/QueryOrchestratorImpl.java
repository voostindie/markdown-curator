package nl.ulso.markdown_curator;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.query.QueryCatalog;
import nl.ulso.markdown_curator.query.QueryResult;
import nl.ulso.markdown_curator.vault.*;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static nl.ulso.hash.ShortHasher.shortHashOf;
import static nl.ulso.markdown_curator.Change.isObjectType;
import static nl.ulso.markdown_curator.vault.Dictionary.emptyDictionary;
import static org.slf4j.LoggerFactory.getLogger;

/// This orchestrator runs queries, of which there can be many, in parallel. In practice there are
/// many queries embedded in documents, while the number of documents that need updating is limited
/// because most queries won't have new output.
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

    /// Run all queries, collect all output, throw away all output except from the ones that
    /// changed, and write the changes back to disk.
    @Override
    public Set<DocumentUpdate> runFor(Changelog changelog)
    {
        LOGGER.debug("Determining impact of the changelog on the documents in the vault.");
        var frontMatterRewrites = frontMatterRewriteResolver.resolveFrontMatterRewrites();
        var documentsToProcess = new HashSet<>(frontMatterRewrites.keySet());
        documentsToProcess.addAll(findImpactedDocuments(changelog));
        LOGGER.info("Selected {} documents to run all queries for.", documentsToProcess.size());
        var queryOutputs = runQueries(documentsToProcess).stream()
            .collect(groupingBy(item -> item.queryBlock().document()));
        var changedDocuments = new HashSet<Document>();
        changedDocuments.addAll(frontMatterRewrites.keySet());
        changedDocuments.addAll(queryOutputs.entrySet().stream()
            .filter(entry -> entry.getValue().stream().anyMatch(QueryOutput::isChanged))
            .map(Map.Entry::getKey).collect(toSet()));
        LOGGER.info("Selected {} documents to rewrite.", changedDocuments.size());
        return changedDocuments.stream()
            .map(document ->
                new DocumentUpdate(
                    document,
                    frontMatterRewrites.getOrDefault(document, emptyDictionary()),
                    queryOutputs.getOrDefault(document, emptyList())
                )
            )
            .collect(toSet());
    }

    /// Find all documents in the vault that are impacted by the changelog.
    ///
    /// If the changelog contains a change to the [Vault], this method returns all documents with a
    /// query block. This change is only published at application startup, and therefore at startup
    /// all queries are run once. This catches any changes that might have happened while the
    /// curator wasn't running. Optionally, the change can also be published if a specific document
    /// in the vault has changed by the user.
    ///
    /// If there is no [Vault] change in the changelog, then this methods returns all documents in
    /// the vault that have a query in them that is impacted by the changelog. To verify that, this
    /// method calls  `isImpactedBy` for each query in each document. There can be many queries, so
    /// this is done in parallel.
    private Set<Document> findImpactedDocuments(Changelog changelog)
    {
        var queryBlocks = vault.findAllQueryBlocks();
        if (changelog.changes().anyMatch(isObjectType(Vault.class)))
        {
            LOGGER.debug(
                "Detected change to the complete vault. Running all queries for all documents.");
            return queryBlocks.stream().map(QueryBlock::document).collect(toSet());
        }
        LOGGER.debug("Verifying impact of the changelog on all documents in the vault");
        var impactedDocuments = ConcurrentHashMap.<Document>newKeySet();
        if (queryBlocks.isEmpty())
        {
            return impactedDocuments;
        }
        var duration = runInParallel(queryBlocks, queryBlock ->
            {
                if (impactedDocuments.contains(queryBlock.document()))
                {
                    LOGGER.debug("Impact on document {} already determined. Skipping validation.",
                        queryBlock.document()
                    );
                    return;
                }
                var hash = queryBlock.outputHash();
                if (hash == null || hash.isEmpty())
                {
                    LOGGER.debug(
                        "Document {} has a query block without output hash. Adding document.",
                        queryBlock.document()
                    );
                    impactedDocuments.add(queryBlock.document());
                    return;
                }
                var query = queryCatalog.query(queryBlock.queryName());
                if (query.isImpactedBy(changelog, queryBlock))
                {
                    LOGGER.debug(
                        "Document {} has a query that might be impacted by the changelog. Adding.",
                        queryBlock.document()
                    );
                    impactedDocuments.add(queryBlock.document());
                }
            }
        );
        LOGGER.info("Validated {} queries in {} ms.", queryBlocks.size(), duration);
        return impactedDocuments;
    }

    /// Run the relevant queries in the vault and collect the queries whose outputs have changed
    /// compared to what's in memory right now.
    ///
    /// Note that we have to collect and keep the output of all queries of the selected documents,
    /// even the ones that didn't change. Query output is not kept in memory between runs. Documents
    /// may embed more than one query, and documents are written to disk as a whole. So the outputs
    /// of all embedded queries of all documents that need an update need to be available.
    ///
    /// The queries are executed in parallel as much as possible because there can be thousands of
    /// them.
    Queue<QueryOutput> runQueries(Set<Document> documents)
    {
        var queryBlocks = findAllQueryBlocksIn(documents);
        var writeQueue = new ConcurrentLinkedQueue<QueryOutput>();
        if (queryBlocks.isEmpty())
        {
            return writeQueue;
        }
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
        LOGGER.info("Executed {} queries in {} ms.", queryBlocks.size(), duration);
        return writeQueue;
    }

    private List<QueryBlock> findAllQueryBlocksIn(Set<Document> documents)
    {
        var queryBlocks = new ArrayList<QueryBlock>();
        for (var document : documents)
        {
            var finder = new QueryBlockFinder();
            document.accept(finder);
            queryBlocks.addAll(finder.queryBlocks());
        }
        return queryBlocks;
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

    private static class QueryBlockFinder
        extends BreadthFirstVaultVisitor
    {
        private final List<QueryBlock> queryBlocks = new ArrayList<>();

        @Override
        public void visit(QueryBlock queryBlock)
        {
            queryBlocks.add(queryBlock);
        }

        List<QueryBlock> queryBlocks()
        {
            return queryBlocks;
        }
    }
}

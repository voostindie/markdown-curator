package nl.ulso.curator.main;

import jakarta.inject.Inject;
import nl.ulso.curator.Curator;
import nl.ulso.curator.RunMode;
import nl.ulso.curator.change.Change;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.vault.*;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.writeString;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.main.DocumentRewriter.rewriteDocument;
import static org.slf4j.LoggerFactory.getLogger;

/// Markdown curator on top of a [Vault] and custom [ChangeProcessor]s and [Query]s.
///
/// Whenever a change in the underlying vault is detected, a three-step process kicks in:
///
/// 1. All relevant [ChangeProcessor]s are executed in the right order by the
/// [ChangeProcessorOrchestrator].
/// 2. All relevant [Query]s are executed by the [QueryOrchestrator].
/// 3. All updated documents resulting from query execution are written to disk.
///
/// Queries are not executed after every detected change. Instead, the running of queries and
/// writing of documents to disk is scheduled to take place after a short delay. If during this
/// delay new changes come in, the task is rescheduled. This prevents superfluous query execution
/// and writes to disk, at the cost of the user having to wait a few seconds after saving the last
/// change. This is especially useful when using Obsidian, which automatically writes changes to
/// disk, every few seconds.
///
/// The query run is performed with the changelog that has been built up from processing all
/// incoming changes. After the queries have finally run and updated documents are written to disk,
/// the changelog is reset.
final class DefaultCurator
    implements Curator, VaultChangedCallback
{
    private static final Logger LOGGER = getLogger(DefaultCurator.class);
    private static final long SCHEDULE_TIMEOUT_IN_SECONDS = 3;

    private final String curatorName;
    private final Vault vault;
    private final ChangeProcessorOrchestrator changeProcessorOrchestrator;
    private final QueryOrchestrator queryOrchestrator;
    private final DocumentPathResolver documentPathResolver;
    private final ScheduledExecutorService delayedExecutor;
    private final Set<String> expectedDocumentUpdates;
    private final List<Change<?>> changeQueue;
    private ScheduledFuture<?> runTask;

    @Inject
    public DefaultCurator(
        Vault vault,
        ChangeProcessorOrchestrator changeProcessorOrchestrator,
        QueryOrchestrator queryOrchestrator, DocumentPathResolver documentPathResolver)
    {
        this.curatorName = Thread.currentThread().getName();
        this.vault = vault;
        this.changeProcessorOrchestrator = changeProcessorOrchestrator;
        this.queryOrchestrator = queryOrchestrator;
        this.documentPathResolver = documentPathResolver;
        this.delayedExecutor = newScheduledThreadPool(1);
        this.expectedDocumentUpdates = new HashSet<>();
        this.changeQueue = new ArrayList<>();
        this.runTask = null;
    }

    @Override
    public void run(RunMode runMode)
    {
        switch (runMode)
        {
            case DAEMON ->
            {
                vaultChanged(create(vault, Vault.class));
                vault.setVaultChangedCallback(this);
                vault.watchForChanges();
            }
            case ONCE ->
            {
                LOGGER.info("Running this curator once.");
                vaultChanged(create(vault, Vault.class));
                cancelQueryWriteRunIfPresent();
                processChangeQueue();
            }
            default -> throw new IllegalArgumentException(
                "Unsupported run mode: " + RunMode.get());
        }
    }

    /// This method is synchronized to ensure it doesn't run concurrently with
    /// [#processChangeQueue].
    @Override
    public synchronized void vaultChanged(Change<?> change)
    {
        LOGGER.info("{} detected for {} '{}'.",
            change.kind(), change.payloadType().getSimpleName(), change.value()
        );
        cancelQueryWriteRunIfPresent();
        changeQueue.addLast(optimizeChangeQueue(change));
        if (canRunImmediatelyFor(change))
        {
            LOGGER.info(
                "Immediately processing all expected document updates.");
            processChangeQueue();
            expectedDocumentUpdates.clear();
        }
        else
        {
            scheduleChangeQueueProcessing();
        }
    }

    /// If the last item in the queue points to the same [Document] as the new change coming in, and
    /// they are both updates, then we can fold them into a single update. This is especially useful
    /// for Obsidian, which updates documents frequently.
    private Change<?> optimizeChangeQueue(Change<?> newChange)
    {
        if (!changeQueue.isEmpty()
            && newChange.payloadType().equals(Document.class)
            && newChange.kind() == Change.Kind.UPDATE)
        {
            var lastChange = changeQueue.getLast();
            if (lastChange.payloadType().equals(Document.class)
                && lastChange.kind() == Change.Kind.UPDATE
                && lastChange.as(Document.class).value().name().
                    equals(newChange.as(Document.class).value().name()))
            {
                LOGGER.debug(
                    "Two consecutive updates to the same document detected. Folding them into a " +
                    "single update.");
                changeQueue.remove(lastChange);
                return Change.update(
                    lastChange.as(Document.class).oldValue(),
                    newChange.as(Document.class).newValue(),
                    Document.class
                );
            }
        }
        return newChange;
    }

    /// If the incoming change is a [Vault] change, that means the application is starting up, and
    /// then there's no reason to wait. If the incoming change is expected as the result of a file
    /// written by the curator, then there's no reason to wait to either. In all cases the curator
    /// should wait for a bit.
    private boolean canRunImmediatelyFor(Change<?> change)
    {
        if (change.payloadType().equals(Vault.class))
        {
            return true;
        }
        if (change.payloadType().equals(Document.class) && change.kind() == Change.Kind.UPDATE)
        {
            return expectedDocumentUpdates.remove(change.as(Document.class).value().name()) &&
                   expectedDocumentUpdates.isEmpty();
        }
        return false;
    }

    /// If there is an incoming change, there's no need to write changes to disk from the previous
    /// processing run, if that exists; a new one will be scheduled shortly.
    private void cancelQueryWriteRunIfPresent()
    {
        if (runTask != null)
        {
            LOGGER.debug("Cancelling currently scheduled task.");
            runTask.cancel(false);
            runTask = null;
        }
    }

    /// A change was detected, which means the queries need to be executed, and changes written to
    /// disk. That work is scheduled for a few seconds from now. If new changes come in in the
    /// meantime, the task will be cancelled and replaced by a new one.
    private void scheduleChangeQueueProcessing()
    {
        LOGGER.debug("Scheduling query processing and document writing task to run in {} seconds.",
            SCHEDULE_TIMEOUT_IN_SECONDS
        );
        runTask = delayedExecutor.schedule(
            this::processChangeQueue,
            SCHEDULE_TIMEOUT_IN_SECONDS,
            SECONDS
        );
    }

    /// This method is synchronized to ensure it doesn't run concurrently with
    /// [#vaultChanged(Change)].
    private synchronized void processChangeQueue()
    {
        MDC.put("curator", curatorName);
        logSeparatorLine();
        var changelog = changeProcessorOrchestrator.runFor(changeQueue);
        queryOrchestrator.runFor(changelog).forEach(this::writeDocument);
        changeQueue.clear();
        logSeparatorLine();
    }

    /// Executing queries has resulted in a set of [DocumentUpdate]s. These must be written to disk.
    /// The writes are registered to detect self-triggered changes later on.
    private void writeDocument(DocumentUpdate documentUpdate)
    {
        var document = documentUpdate.document();
        LOGGER.info("Rewriting document: '{}'.", document);
        var newDocumentContent = rewriteDocument(documentUpdate);
        try
        {
            var path = documentPathResolver.resolveAbsolutePath(document);
            if (document.lastModified() != getLastModifiedTime(path).toMillis())
            {
                LOGGER.warn("Document '{}' has changed on disk. Skipping.", document);
                return;
            }
            writeString(path, newDocumentContent);
            expectedDocumentUpdates.add(document.name());
        }
        catch (IOException e)
        {
            LOGGER.warn("Couldn't write document '{}' to disk.", document);
            LOGGER.error(e.getMessage(), e);
        }
    }

    private void logSeparatorLine()
    {
        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info("-".repeat(80));
        }
    }
}

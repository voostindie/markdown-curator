package nl.ulso.curator.main;

import jakarta.inject.Inject;
import nl.ulso.curator.Curator;
import nl.ulso.curator.changelog.*;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.vault.*;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.writeString;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static nl.ulso.curator.changelog.Change.Kind.UPDATE;
import static nl.ulso.curator.changelog.Change.create;
import static nl.ulso.curator.changelog.Changelog.emptyChangelog;
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
final class CuratorImpl
    implements Curator, VaultChangedCallback
{
    private static final Logger LOGGER = getLogger(CuratorImpl.class);
    private static final long SCHEDULE_TIMEOUT_IN_SECONDS = 3;

    private final String curatorName;
    private final Vault vault;
    private final ChangeProcessorOrchestrator changeProcessorOrchestrator;
    private final QueryOrchestrator queryOrchestrator;
    private final DocumentPathResolver documentPathResolver;
    private final Map<String, Long> writtenDocuments;
    private final ScheduledExecutorService delayedExecutor;
    private Changelog backlog;
    private ScheduledFuture<?> runTask;

    @Inject
    public CuratorImpl(
        Vault vault,
        ChangeProcessorOrchestrator changeProcessorOrchestrator,
        QueryOrchestrator queryOrchestrator, DocumentPathResolver documentPathResolver)
    {
        this.curatorName = Thread.currentThread().getName();
        this.vault = vault;
        this.changeProcessorOrchestrator = changeProcessorOrchestrator;
        this.queryOrchestrator = queryOrchestrator;
        this.documentPathResolver = documentPathResolver;
        this.writtenDocuments = new HashMap<>();
        this.delayedExecutor = newScheduledThreadPool(1);
        this.backlog = emptyChangelog();
        this.runTask = null;
    }

    @Override
    public void runOnce()
    {
        LOGGER.info("Running this curator once.");
        vaultChanged(create(vault, Vault.class));
        cancelQueryWriteRunIfPresent();
        performQueryWriteRun();
    }

    @Override
    public void run()
    {
        vaultChanged(create(vault, Vault.class));
        vault.setVaultChangedCallback(this);
        vault.watchForChanges();
    }

    /// This method is synchronized to ensure it doesn't run concurrently with
    /// [#performQueryWriteRun].
    @Override
    public synchronized void vaultChanged(Change<?> change)
    {
        if (checkSelfTriggeredUpdate(change))
        {
            return;
        }
        LOGGER.info("{} detected for {} '{}'.",
            change.kind(), change.payloadType().getSimpleName(), change.value()
        );
        cancelQueryWriteRunIfPresent();
        backlog = backlog.append(changeProcessorOrchestrator.runFor(change));
        scheduleQueryWriteRun();
    }

    /// A change is self-triggered if it was the result of the curator writing a file to disk. To
    /// cancel out those changes - no query will produce different output, guaranteed - the curator
    /// keeps track of the files it writes and then compares them with the changes it detects. If
    /// they match, the change was self-triggered.
    ///
    /// @param change Change that triggered the curator.
    /// @return `true` if the change was self-triggered, `false` otherwise.
    private boolean checkSelfTriggeredUpdate(Change<?> change)
    {
        if (!(change.payloadType().equals(Document.class) && change.kind() == UPDATE))
        {
            return false;
        }
        var document = change.as(Document.class).value();
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
        LOGGER.debug("Ignoring change on document '{}' because this curator caused it.",
            documentName
        );
        return true;
    }

    /// If there is an incoming change, there's no need to write changes to disk from the previous
    /// processing run, if that exists; a new one will be scheduled shortly.
    private void cancelQueryWriteRunIfPresent()
    {
        if (runTask != null)
        {
            LOGGER.debug("Cancelling currently scheduled task.");
            runTask.cancel(false);
        }
    }

    /// A change was detected, which means the queries need to be executed, and changes written to
    /// disk. That work is scheduled for a few seconds from now. If new changes come in in the
    /// meantime, the task will be cancelled and replaced by a new one.
    private void scheduleQueryWriteRun()
    {
        LOGGER.debug("Scheduling query processing and document writing task to run in {} seconds.",
            SCHEDULE_TIMEOUT_IN_SECONDS
        );
        runTask = delayedExecutor.schedule(
            this::performQueryWriteRun,
            SCHEDULE_TIMEOUT_IN_SECONDS, SECONDS
        );
    }

    /// This method is synchronized to ensure it doesn't run concurrently with
    /// [#vaultChanged(Change)].
    private synchronized void performQueryWriteRun()
    {
        MDC.put("curator", curatorName);
        LOGGER.info("-".repeat(80));
        queryOrchestrator.runFor(backlog).forEach(this::writeDocument);
        backlog = emptyChangelog();
        LOGGER.info("-".repeat(80));
    }

    /// Executing queries has resulted in a set of [DocumentUpdate]s. These must be written to
    /// disk. The writes are registered to detect self-triggered changes later on.
    private void writeDocument(DocumentUpdate documentUpdate)
    {
        var document = documentUpdate.document();
        LOGGER.info("Rewriting document: {}", documentUpdate.document());
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
            writtenDocuments.put(document.name(), getLastModifiedTime(path).toMillis());
        }
        catch (IOException e)
        {
            LOGGER.warn("Couldn't write document '{}' to disk.", document);
            LOGGER.error(e.getMessage(), e);
        }
    }
}

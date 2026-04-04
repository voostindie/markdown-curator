package nl.ulso.curator.vault;

import io.methvin.watcher.DirectoryChangeEvent;
import org.slf4j.Logger;

import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.vault.FileSystemVault.newDocumentFromAbsolutePath;
import static org.slf4j.LoggerFactory.getLogger;

/// Handles file modification events and applies them to a [FileSystemFolder].
final class FileModifiedEventHandler
    implements DirectoryChangeEventHandler
{
    private static final Logger LOGGER = getLogger(FileModifiedEventHandler.class);

    @Override
    public void handle(
        DirectoryChangeEvent event, FileSystemFolder parent,
        VaultChangedCallback callback)
    {
        var eventAbsolutePath = event.path();
        var newDocument = newDocumentFromAbsolutePath(eventAbsolutePath);
        LOGGER.debug("Detected changes to document '{}'.", newDocument);
        var oldDocument = parent.addDocument(newDocument);
        callback.vaultChanged(update(oldDocument, newDocument, Document.class));
    }
}

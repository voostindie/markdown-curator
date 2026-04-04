package nl.ulso.curator.vault;

import io.methvin.watcher.DirectoryChangeEvent;
import org.slf4j.Logger;

import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.vault.FileSystemVault.documentName;
import static org.slf4j.LoggerFactory.getLogger;

/// Handles file deletion events and applies them to a [FileSystemFolder].
final class FileDeletedEventHandler
    implements DirectoryChangeEventHandler
{
    private static final Logger LOGGER = getLogger(FileDeletedEventHandler.class);

    @Override
    public void handle(
        DirectoryChangeEvent event, FileSystemFolder parent,
        VaultChangedCallback callback)
    {
        var eventAbsolutePath = event.path();
        var name = documentName(eventAbsolutePath);
        parent.document(name).ifPresent(document ->
        {
            LOGGER.debug("Deleted document '{}'.", name);
            parent.removeDocument(name);
            callback.vaultChanged(delete(document, Document.class));
        });
    }
}

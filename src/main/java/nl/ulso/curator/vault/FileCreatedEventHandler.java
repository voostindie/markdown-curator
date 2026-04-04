package nl.ulso.curator.vault;

import io.methvin.watcher.DirectoryChangeEvent;
import org.slf4j.Logger;

import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.vault.FileSystemVault.newDocumentFromAbsolutePath;
import static org.slf4j.LoggerFactory.getLogger;

/// Handles file creation events and applies them to a [FileSystemFolder].
final class FileCreatedEventHandler
    implements DirectoryChangeEventHandler
{
    private static final Logger LOGGER = getLogger(FileCreatedEventHandler.class);

    @Override
    public void handle(
        DirectoryChangeEvent event, FileSystemFolder parent,
        VaultChangedCallback callback)
    {
        var eventAbsolutePath = event.path();
        var document = newDocumentFromAbsolutePath(eventAbsolutePath);
        LOGGER.debug("Detected new document '{}'.", document);
        parent.addDocument(document);
        callback.vaultChanged(create(document, Document.class));
    }
}

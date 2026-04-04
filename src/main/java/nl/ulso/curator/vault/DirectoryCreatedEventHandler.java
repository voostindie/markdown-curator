package nl.ulso.curator.vault;

import io.methvin.watcher.DirectoryChangeEvent;
import org.slf4j.Logger;

import java.io.IOException;

import static java.nio.file.Files.walkFileTree;
import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.vault.FileSystemVault.folderName;
import static nl.ulso.curator.vault.FileSystemVault.isHidden;
import static org.slf4j.LoggerFactory.getLogger;

/// Handles directory creation events and applies them to a [FileSystemFolder].
///
/// When a directory is created on disk, it might be an existing directory moving into the vault, or
/// a rename. In both cases we need to recursively detect its contents and produce CREATE events for
/// every file (if it is a document) and subdirectory (if it is a valid folder).
final class DirectoryCreatedEventHandler
    implements DirectoryChangeEventHandler
{
    private static final Logger LOGGER = getLogger(DirectoryCreatedEventHandler.class);

    @Override
    public void handle(
        DirectoryChangeEvent event, FileSystemFolder parent, VaultChangedCallback callback)
    {
        var eventAbsolutePath = event.path();
        if (isHidden(eventAbsolutePath))
        {
            return;
        }
        var folder = parent.addFolder(folderName(eventAbsolutePath));
        LOGGER.debug("Detected new folder '{}'.", folder);
        callback.vaultChanged(create(folder, Folder.class));
        try
        {
            walkFileTree(
                eventAbsolutePath,
                new FileSystemVault.VaultBuilder(folder, eventAbsolutePath, callback)
            );
        }
        catch (IOException e)
        {
            LOGGER.warn("Error while processing file tree.", e);
        }
    }
}

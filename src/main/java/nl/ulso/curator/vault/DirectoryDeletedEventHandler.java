package nl.ulso.curator.vault;

import io.methvin.watcher.DirectoryChangeEvent;
import org.slf4j.Logger;

import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.vault.FileSystemVault.folderName;
import static org.slf4j.LoggerFactory.getLogger;

/// Handles directory deletion events and applies them to a [FileSystemFolder].
///
/// When a directory is deleted on disk, it might map to a folder that contains documents and
/// subfolders. For each entry a DELETE event needs to be produced.
///
/// Note that on deletion of a folder, the directory watcher actually generates a deletion event for
/// every file and folder in it. However, these come in the wrong order, after the in-memory
/// representation of the folder has already been processed. They end up being ignored.
final class DirectoryDeletedEventHandler
    implements DirectoryChangeEventHandler
{
    private static final Logger LOGGER = getLogger(DirectoryDeletedEventHandler.class);

    @Override
    public void handle(
        DirectoryChangeEvent event, FileSystemFolder parent,
        VaultChangedCallback callback)
    {
        var eventAbsolutePath = event.path();
        var name = folderName(eventAbsolutePath);
        parent.folder(name).ifPresent(folder ->
        {
            LOGGER.debug("Deleted folder '{}'.", name);
            parent.removeFolder(name);
            var deleter = new RecursiveFolderDeleter(callback);
            folder.accept(deleter);
        });
    }

    private static class RecursiveFolderDeleter
        extends BreadthFirstVaultVisitor
    {
        private final VaultChangedCallback vaultChangedCallback;

        private RecursiveFolderDeleter(VaultChangedCallback callback)
        {
            vaultChangedCallback = callback;
        }

        @Override
        public void visit(Folder folder)
        {
            super.visit(folder);
            vaultChangedCallback.vaultChanged(delete(folder, Folder.class));
        }

        @Override
        public void visit(Document document)
        {
            vaultChangedCallback.vaultChanged(delete(document, Document.class));
        }
    }
}

package nl.ulso.obsidian.watcher.vault;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static nl.ulso.obsidian.watcher.vault.Document.newDocument;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link Vault} implementation on top of the (default) filesystem.
 * <p/>
 * On creation it uses a {@link FileVisitor} to process all folders and documents and pull them
 * in memory. From then on it watches all folders and subfolders for changes using the file system's
 * {@link WatchService}.
 */
public abstract class FileSystemVault
        extends Folder
        implements Vault
{
    private static final Logger LOGGER = getLogger(FileSystemVault.class);

    private final WatchService watchService;
    private final Map<WatchKey, Path> watchKeys;
    private final Path absolutePath;

    public FileSystemVault(Path absolutePath)
            throws IOException
    {
        super(null, absolutePath.toString());
        this.absolutePath = absolutePath;
        try
        {
            watchService = FileSystems.getDefault().newWatchService();
        }
        catch (IOException e)
        {
            throw new RuntimeException(
                    "Could not create a WatchService on the default filesystem", e);
        }
        watchKeys = new HashMap<>();
        Files.walkFileTree(absolutePath, new VaultVisitor(this, absolutePath));
    }

    @Override
    public void accept(Visitor visitor)
    {
        visitor.visitVault(this);
    }

    @Override
    public void watchForChanges()
            throws InterruptedException, IOException
    {
        LOGGER.info("Watching {} for changes", absolutePath);
        WatchKey key;
        while ((key = watchService.take()) != null)
        {
            for (WatchEvent<?> event : key.pollEvents())
            {
                Path relativePath = (Path) event.context();
                Path absolutePath = watchKeys.get(key).resolve(relativePath);
                processFileSystemEvent(absolutePath, event.kind());
            }
            key.reset();
        }
    }

    private void processFileSystemEvent(Path absolutePath, WatchEvent.Kind<?> event)
            throws IOException
    {
        var parent = resolveParentFolder(absolutePath);
        if (event == ENTRY_CREATE)
        {
            processFileCreationEvent(absolutePath, parent);
        }
        else if (event == ENTRY_MODIFY)
        {
            processFileModificationEvent(absolutePath, parent);
        }
        else if (event == ENTRY_DELETE)
        {
            processFileDeletionEvent(absolutePath, parent);
        }
    }

    private void processFileCreationEvent(Path absolutePath, Folder parent)
            throws IOException
    {
        if (Files.isDirectory(absolutePath) && !isHidden(absolutePath))
        {
            var folder = parent.addFolder(folderName(absolutePath));
            LOGGER.info("Detected new folder: {}", folder.name());
            Files.walkFileTree(absolutePath, new VaultVisitor(folder, absolutePath));
        }
        else if (isDocument(absolutePath))
        {
            var document = newDocumentFromAbsolutePath(absolutePath);
            LOGGER.info("Detected new document: {}", document.name());
            parent.addDocument(document);
        }
    }

    private void processFileModificationEvent(Path absolutePath, Folder parent)
    {
        if (isDocument(absolutePath))
        {
            var document = newDocumentFromAbsolutePath(absolutePath);
            LOGGER.info("Detected changed document: {}", document.name());
            parent.addDocument(document);
        }
    }

    private void processFileDeletionEvent(Path absolutePath, Folder parent)
    {
        if (isDocument(absolutePath))
        {
            String name = documentName(absolutePath);
            LOGGER.info("Detected deleted document: {}", name);
            parent.removeDocument(name);
        }
        else
        {
            String name = folderName(absolutePath);
            var folder = parent.folder(name);
            if (folder.isPresent())
            {
                LOGGER.info("Detected deleted folder: {}", name);
                parent.removeFolder(name);
            }
        }
    }

    private Folder resolveParentFolder(Path absolutePath)
    {
        var relativePath = this.absolutePath.relativize(absolutePath);
        var steps = relativePath.getNameCount() - 1;
        Folder folder = this;
        for (int i = 0; i < steps; i++)
        {
            folder = folder(relativePath.getName(i).toString()).orElseThrow();
        }
        return folder;
    }

    private boolean isHidden(Path directory)
    {
        return directory.getFileName().toString().startsWith(".");
    }

    private boolean isDocument(Path file)
    {
        return file.getFileName().toString().endsWith(".md");
    }

    private Document newDocumentFromAbsolutePath(Path absolutePath)
    {
        try
        {
            List<String> lines = Files.readAllLines(absolutePath);
            return newDocument(documentName(absolutePath), lines);
        }
        catch (IOException e)
        {
            LOGGER.error("Could not read file {}", absolutePath, e);
            throw new IllegalStateException("Could not read file " + absolutePath, e);
        }
    }

    private String folderName(Path absolutePath)
    {
        return absolutePath.getFileName().toString();
    }

    private String documentName(Path absolutePath)
    {
        var fileName = absolutePath.getFileName().toString();
        var extensionIndex = fileName.lastIndexOf('.');
        var endIndex = extensionIndex != -1 ? extensionIndex : fileName.length();
        return fileName.substring(0, endIndex);
    }

    private class VaultVisitor
            extends SimpleFileVisitor<Path>
    {
        private final Path root;
        private Folder currentFolder;

        private VaultVisitor(Folder targetFolder, Path absolutePath)
        {
            root = absolutePath;
            currentFolder = targetFolder;
        }

        @Override
        public FileVisitResult preVisitDirectory(
                Path directory, BasicFileAttributes attributes)
                throws IOException
        {
            if (isHidden(directory))
            {
                LOGGER.debug("Skipping directory {}", directory);
                return FileVisitResult.SKIP_SUBTREE;
            }
            var watchKey =
                    directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watchKeys.put(watchKey, directory);
            LOGGER.debug("Watching directory: {}", directory);
            if (!root.equals(directory))
            {
                currentFolder = currentFolder.addFolder(folderName(directory));
            }
            return super.preVisitDirectory(directory, attributes);
        }

        @Override
        public FileVisitResult visitFile(
                Path file, BasicFileAttributes attributes)
                throws IOException
        {
            if (isDocument(file))
            {
                currentFolder.addDocument(newDocumentFromAbsolutePath(file));
            }
            return super.visitFile(file, attributes);
        }

        @Override
        public FileVisitResult postVisitDirectory(
                Path directory, IOException exception)
                throws IOException
        {
            if (exception != null)
            {
                LOGGER.warn(
                        "Traversing {} threw an exception; this might lead to unexpected behavior",
                        directory, exception);
            }
            if (!root.equals(directory))
            {
                currentFolder = currentFolder.parent();
            }
            return super.postVisitDirectory(directory, exception);
        }
    }
}

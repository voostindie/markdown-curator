package nl.ulso.macu.vault;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link Vault} implementation on top of the (default) filesystem.
 * <p/>
 * On creation it uses a {@link FileVisitor} to process all folders and documents and pull them
 * in memory. From then on it watches all folders and subfolders for changes using the file system's
 * {@link WatchService}.
 */
public final class FileSystemVault
        extends FileSystemFolder
        implements Vault
{
    private static final Logger LOGGER = getLogger(FileSystemVault.class);

    private final WatchService watchService;
    private final Map<WatchKey, Path> watchKeys;
    private final Path absolutePath;
    private VaultChangedCallback callback;

    public FileSystemVault(Path absolutePath)
            throws IOException
    {
        super(absolutePath.toString());
        this.callback = () -> { /* Default: No-op */ };
        this.absolutePath = absolutePath;
        try
        {
            watchService = absolutePath.getFileSystem().newWatchService();
        }
        catch (IOException e)
        {
            throw new RuntimeException(
                    "Could not create a WatchService on the filesystem", e);
        }
        watchKeys = new HashMap<>();
        walkFileTree(absolutePath, new VaultBuilder(this, absolutePath));
        if (LOGGER.isInfoEnabled())
        {
            var statistics = ElementCounter.countFoldersAndDocuments(this);
            LOGGER.info("Read vault {} into memory with {} folders and {} documents", name(),
                    statistics.folders, statistics.documents);
        }
    }

    @Override
    public void setVaultChangedCallback(VaultChangedCallback callback)
    {
        this.callback = requireNonNull(callback);
    }

    @Override
    public void accept(VaultVisitor visitor)
    {
        visitor.visit(this);
    }

    @Override
    public Collection<QueryBlock> findAllQueryBlocks()
    {
        var finder = new QueryBlockFinder();
        this.accept(finder);
        return finder.queries();
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
            callback.vaultChanged();
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

    private void processFileCreationEvent(Path absolutePath, FileSystemFolder parent)
            throws IOException
    {
        if (Files.isDirectory(absolutePath) && !isHidden(absolutePath))
        {
            var folder = parent.addFolder(folderName(absolutePath));
            LOGGER.info("Detected new folder: {}", folder.name());
            walkFileTree(absolutePath, new VaultBuilder(folder, absolutePath));
        }
        else if (isDocument(absolutePath))
        {
            var document = newDocumentFromAbsolutePath(absolutePath);
            LOGGER.info("Detected new document: {}", document.name());
            parent.addDocument(document);
        }
    }

    private void processFileModificationEvent(Path absolutePath, FileSystemFolder parent)
    {
        if (isDocument(absolutePath))
        {
            var document = newDocumentFromAbsolutePath(absolutePath);
            LOGGER.info("Detected changed document: {}", document.name());
            parent.addDocument(document);
        }
    }

    private void processFileDeletionEvent(Path absolutePath, FileSystemFolder parent)
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

    private FileSystemFolder resolveParentFolder(Path absolutePath)
    {
        var relativePath = this.absolutePath.relativize(absolutePath);
        var steps = relativePath.getNameCount() - 1;
        Folder folder = this;
        for (int i = 0; i < steps; i++)
        {
            folder = folder(relativePath.getName(i).toString()).orElseThrow();
        }
        return (FileSystemFolder) folder;
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
            return Document.newDocument(documentName(absolutePath),
                    Files.readAllLines(absolutePath));
        }
        catch (IOException e)
        {
            LOGGER.error("Could not read file from disk: {}", absolutePath, e);
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

    public Path resolveAbsolutePath(Document document)
    {
        var parents = new ArrayList<Folder>();
        var folder = document.folder();
        while (folder != this)
        {
            parents.add(folder);
            folder = folder.parent();
        }
        Collections.reverse(parents);
        Path absolutePath = this.absolutePath;
        for (Folder parent : parents)
        {
            absolutePath = absolutePath.resolve(parent.name());
        }
        absolutePath = absolutePath.resolve(document.name() + ".md");
        LOGGER.debug("Resolved absolute path for document '{}': {}", document, absolutePath);
        return absolutePath;
    }

    private class VaultBuilder
            extends SimpleFileVisitor<Path>
    {
        private final Path root;
        private FileSystemFolder currentFolder;

        private VaultBuilder(FileSystemFolder targetFolder, Path absolutePath)
        {
            root = absolutePath;
            currentFolder = targetFolder;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
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
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                throws IOException
        {
            if (isDocument(file))
            {
                currentFolder.addDocument(newDocumentFromAbsolutePath(file));
            }
            return super.visitFile(file, attributes);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path directory, IOException exception)
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

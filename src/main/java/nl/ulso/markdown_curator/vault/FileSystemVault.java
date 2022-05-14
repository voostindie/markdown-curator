package nl.ulso.markdown_curator.vault;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHasher;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.Files.walkFileTree;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link Vault} implementation on top of the (default) filesystem.
 * <p/>
 * On creation, it uses a {@link FileVisitor} to process all folders and documents and pull them
 * in memory. From then on it watches all folders and subfolders for changes using the file system's
 * {@link WatchService}.
 */
public final class FileSystemVault
        extends FileSystemFolder
        implements Vault
{
    private static final Logger LOGGER = getLogger(FileSystemVault.class);

    private final Path absolutePath;
    private final DirectoryWatcher watcher;
    private VaultChangedCallback callback;

    public FileSystemVault(Path absolutePath)
            throws IOException
    {
        // This forces the directory watcher to deduce which WatchService to use.
        // On macOS, this results in a native, non-polling service. Nice and fast.
        // However, this service doesn't work with the JimFS filesystem, used in tests.
        // That's what the other constructor is for.
        this(absolutePath, null);
    }

    public FileSystemVault(Path absolutePath, WatchService watchService)
            throws IOException
    {
        super(absolutePath.toString());
        this.callback = () -> { /* Default: No-op */ };
        this.absolutePath = absolutePath;
        VaultBuilder vaultBuilder = new VaultBuilder(this, absolutePath);
        walkFileTree(absolutePath, vaultBuilder);
        this.watcher = DirectoryWatcher.builder()
                .path(absolutePath)
                .listener(this::processFileSystemEvent)
                .watchService(watchService)
                .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                .build();
        if (LOGGER.isInfoEnabled())
        {
            var statistics = ElementCounter.countFoldersAndDocuments(this);
            LOGGER.info("Read vault {} into memory with {} folders and {} documents", name(),
                    statistics.folders(), statistics.documents());
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
    {
        LOGGER.info("Watching {} for changes", absolutePath);
        watcher.watch();
    }

    private void processFileSystemEvent(DirectoryChangeEvent event)
    {
        var eventAbsolutePath = event.path();
        LOGGER.debug("Change detected: {}", eventAbsolutePath);
        var parent = resolveParentFolder(eventAbsolutePath);
        if (parent != null)
        {
            switch (event.eventType())
            {
                case CREATE -> processFileCreationEvent(event, parent);
                case DELETE -> processFileDeletionEvent(event, parent);
                case MODIFY -> processFileModificationEvent(event, parent);
                default -> LOGGER.warn("Unsupported filesystem event {}", event.eventType());
            }
            callback.vaultChanged();
        }
    }

    private void processFileCreationEvent(DirectoryChangeEvent event, FileSystemFolder parent)
    {
        var eventAbsolutePath = event.path();
        if (event.isDirectory() && !isHidden(eventAbsolutePath))
        {
            var folder = parent.addFolder(folderName(eventAbsolutePath));
            LOGGER.info("Detected new folder: {}", folder);
            try
            {
                walkFileTree(eventAbsolutePath, new VaultBuilder(folder, eventAbsolutePath));
            }
            catch (IOException e)
            {
                LOGGER.warn("Error while processing file tree", e);
            }
        }
        else if (isDocument(eventAbsolutePath))
        {
            var document = newDocumentFromAbsolutePath(eventAbsolutePath);
            LOGGER.info("Detected new document: {}", document);
            parent.addDocument(document);
        }
    }

    private void processFileModificationEvent(DirectoryChangeEvent event, FileSystemFolder parent)
    {
        var eventAbsolutePath = event.path();
        if (isDocument(eventAbsolutePath))
        {
            var document = newDocumentFromAbsolutePath(eventAbsolutePath);
            LOGGER.info("Detected changes to document: {}", document);
            parent.addDocument(document);
        }
    }

    private void processFileDeletionEvent(DirectoryChangeEvent event, FileSystemFolder parent)
    {
        var eventAbsolutePath = event.path();
        if (isDocument(eventAbsolutePath))
        {
            var name = documentName(eventAbsolutePath);
            LOGGER.info("Document deleted: {}", name);
            parent.removeDocument(name);
        }
        else
        {
            var name = folderName(eventAbsolutePath);
            var folder = parent.folder(name);
            if (folder.isPresent())
            {
                LOGGER.info("Folder deleted: {}", name);
                parent.removeFolder(name);
            }
        }
    }

    private FileSystemFolder resolveParentFolder(Path eventAbsolutePath)
    {
        var relativePath = absolutePath.relativize(eventAbsolutePath);
        var steps = relativePath.getNameCount() - 1;
        Folder folder = this;
        for (int i = 0; i < steps; i++)
        {

            Path directory = relativePath.getName(i);
            if (isHidden(directory))
            {
                LOGGER.debug("Hidden directory. Doing nothing: {}", relativePath);
                return null;
            }
            var subfolder = folder.folder(folderName(directory)).orElse(null);
            if (subfolder == null)
            {
                LOGGER.debug("Couldn't find subfolder '{}' in folder '{}'. Doing nothing.",
                        directory, folder.name());
                return null;
            }
            folder = subfolder;
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
        var path = this.absolutePath;
        for (Folder parent : parents)
        {
            path = path.resolve(parent.name());
        }
        path = path.resolve(document.name() + ".md");
        LOGGER.debug("Resolved absolute path for document '{}': {}", document, path);
        return path;
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

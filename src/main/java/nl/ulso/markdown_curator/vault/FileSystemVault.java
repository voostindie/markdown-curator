package nl.ulso.markdown_curator.vault;

import com.google.inject.Inject;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.hashing.FileHasher;
import nl.ulso.markdown_curator.DocumentPathResolver;
import nl.ulso.markdown_curator.VaultPath;
import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.slf4j.Logger;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

import static java.nio.file.Files.walkFileTree;
import static java.util.Collections.reverse;
import static java.util.Objects.requireNonNull;
import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link Vault} implementation on top of the (default) filesystem.
 * <p/>
 * On creation, it uses a {@link FileVisitor} to process all folders and documents and pull them
 * in memory. From then on it watches all folders and subfolders for changes using the file system's
 * {@link WatchService}.
 */
@Singleton
public final class FileSystemVault
        extends FileSystemFolder
        implements Vault, DocumentPathResolver
{
    private static final Logger LOGGER = getLogger(FileSystemVault.class);

    private final Path absolutePath;
    private final DirectoryWatcher watcher;
    private VaultChangedCallback callback;

    @Inject
    public FileSystemVault(@VaultPath Path absolutePath, WatchServiceHolder watchServiceHolder)
            throws IOException
    {
        super(absolutePath.toString());
        this.callback = (VaultChangedEvent changeEvent) -> { /* Default: No-op */ };
        this.absolutePath = absolutePath;
        VaultBuilder vaultBuilder = new VaultBuilder(this, absolutePath);
        walkFileTree(absolutePath, vaultBuilder);
        this.watcher = DirectoryWatcher.builder()
                .path(absolutePath)
                .listener(this::processFileSystemEvent)
                .watchService(watchServiceHolder.watchService)
                .fileHasher(FileHasher.LAST_MODIFIED_TIME)
                .build();
        if (LOGGER.isInfoEnabled())
        {
            var statistics = ElementCounter.countFoldersAndDocuments(this);
            LOGGER.info("Read vault {} into memory with {} folders and {} documents", name(),
                    statistics.folders(), statistics.documents());
        }
    }

    public FileSystemVault(Path absolutePath)
            throws IOException
    {
        // This forces the directory watcher to deduce itself which WatchService to use.
        // On macOS, this results in a native, non-polling service. Nice and fast.
        // However, this service doesn't work with the JimFS filesystem, used in tests.
        // That's why there's a constructor for an "optional" WatchService.
        this(absolutePath, new WatchServiceHolder());
    }

    static class WatchServiceHolder
    {
        @Inject(optional = true)
        WatchService watchService = null;
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
            VaultChangedEvent output = null;
            switch (event.eventType())
            {
                case CREATE -> output = processFileCreationEvent(event, parent);
                case DELETE -> output = processFileDeletionEvent(event, parent);
                case MODIFY -> output = processFileModificationEvent(event, parent);
                default -> LOGGER.warn("Unsupported filesystem event {}", event.eventType());
            }
            if (output != null)
            {
                callback.vaultChanged(output);
            }
        }
    }

    private VaultChangedEvent processFileCreationEvent(
            DirectoryChangeEvent event, FileSystemFolder parent)
    {
        var eventAbsolutePath = event.path();
        if (event.isDirectory() && !isHidden(eventAbsolutePath))
        {
            var folder = parent.addFolder(folderName(eventAbsolutePath));
            LOGGER.info("Detected new folder: {}", folder);
            try
            {
                walkFileTree(eventAbsolutePath, new VaultBuilder(folder, eventAbsolutePath));
                return folderAdded(folder);
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
            return documentAdded(document);
        }
        return null;
    }

    private VaultChangedEvent processFileModificationEvent(
            DirectoryChangeEvent event, FileSystemFolder parent)
    {
        var eventAbsolutePath = event.path();
        if (isDocument(eventAbsolutePath))
        {
            var document = newDocumentFromAbsolutePath(eventAbsolutePath);
            LOGGER.info("Detected changes to document: {}", document);
            parent.addDocument(document);
            return documentChanged(document);
        }
        return null;
    }

    private VaultChangedEvent processFileDeletionEvent(
            DirectoryChangeEvent event, FileSystemFolder parent)
    {
        var eventAbsolutePath = event.path();
        if (isDocument(eventAbsolutePath))
        {
            var name = documentName(eventAbsolutePath);
            return parent.document(name).map(document ->
            {
                LOGGER.info("Document deleted: {}", name);
                parent.removeDocument(name);
                return documentRemoved(document);
            }).orElse(null);
        }
        else
        {
            var name = folderName(eventAbsolutePath);
            return parent.folder(name).map(folder ->
            {
                LOGGER.info("Folder deleted: {}", name);
                parent.removeFolder(name);
                return folderRemoved(folder);
            }).orElse(null);
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
                        directory, folder);
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
            var lastModified = Files.getLastModifiedTime(absolutePath).toMillis();
            return Document.newDocument(documentName(absolutePath), lastModified,
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
        reverse(parents);
        var path = this.absolutePath;
        for (Folder parent : parents)
        {
            path = path.resolve(parent.name());
        }
        path = path.resolve(document.name() + ".md");
        LOGGER.debug("Resolved absolute path for document: {}", path);
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

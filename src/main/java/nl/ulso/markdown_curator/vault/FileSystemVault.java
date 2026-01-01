package nl.ulso.markdown_curator.vault;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.Change;
import nl.ulso.markdown_curator.DocumentPathResolver;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static io.methvin.watcher.hashing.FileHasher.DEFAULT_FILE_HASHER;
import static io.methvin.watcher.hashing.FileHasher.LAST_MODIFIED_TIME;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.readAllLines;
import static java.nio.file.Files.walkFileTree;
import static java.text.Normalizer.Form.NFC;
import static java.text.Normalizer.normalize;
import static java.util.Collections.reverse;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static nl.ulso.markdown_curator.Change.creation;
import static nl.ulso.markdown_curator.Change.deletion;
import static nl.ulso.markdown_curator.Change.modification;
import static nl.ulso.markdown_curator.vault.Document.newDocument;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link Vault} implementation on top of the (default) filesystem.
 * <p/>
 * On creation, it uses a {@link FileVisitor} to process all folders and documents and pull them in
 * memory. From then on it watches all folders and subfolders for changes using the file system's
 * {@link WatchService}.
 */
@Singleton
public final class FileSystemVault
    extends FileSystemFolder
    implements Vault, DocumentPathResolver, VaultRefresher
{
    private static final Logger LOGGER = getLogger(FileSystemVault.class);

    private final Path absolutePath;
    private final DirectoryWatcher watcher;
    private VaultChangedCallback callback;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    public FileSystemVault(Path absolutePath, Optional<WatchService> watchService)
    {
        super(absolutePath.toString());
        this.callback = (Change<?> _) -> { /* Default: No-op */ };
        this.absolutePath = absolutePath;
        VaultBuilder vaultBuilder = new VaultBuilder(this, absolutePath);
        try
        {
            walkFileTree(absolutePath, vaultBuilder);
            // On macOS, use a faster hasher, based on file timestamps instead of contents.
            // From the README on https://github.com/gmethvin/directory-watcher:
            // "This hasher is only suitable for platforms that have at least millisecond precision
            // in last modified times from Java. It's known to work with JDK 10+ on Macs with APFS."
            var fileHasher = System.getProperty("os.name").contains("Mac OS X")
                             ? LAST_MODIFIED_TIME
                             : DEFAULT_FILE_HASHER;
            this.watcher = DirectoryWatcher.builder()
                .path(absolutePath)
                .listener(this::processFileSystemEvent)
                .watchService(watchService.orElse(null))
                .fileHasher(fileHasher)
                .build();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not instantiate vault on path '"
                                            + absolutePath + "'", e
            );
        }
        if (LOGGER.isInfoEnabled())
        {
            var statistics = ElementCounter.countFoldersAndDocuments(this);
            LOGGER.info("Read vault {} into memory with {} folders and {} documents", name(),
                statistics.folders(), statistics.documents()
            );
        }
    }

    public FileSystemVault(Path absolutePath)
    {
        // This forces the directory watcher to deduce itself which WatchService to use.
        // On macOS, this results in a native, non-polling service. Nice and fast.
        // However, this service doesn't work with the JimFS filesystem, used in tests.
        // That's why there's a constructor for an "optional" WatchService.
        this(absolutePath, empty());
    }

    public Path root()
    {
        return absolutePath;
    }

    @Override
    public void setVaultChangedCallback(VaultChangedCallback callback)
    {
        this.callback = requireNonNull(callback);
    }

    @Override
    public void triggerRefresh()
    {
        callback.vaultChanged(modification(this, Vault.class));
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
        accept(finder);
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
        LOGGER.trace("Change detected: {}", eventAbsolutePath);
        var parent = resolveParentFolder(eventAbsolutePath);
        if (parent != null)
        {
            var change = switch (event.eventType())
            {
                case CREATE -> processFileCreationEvent(event, parent);
                case DELETE -> processFileDeletionEvent(event, parent);
                case MODIFY -> processFileModificationEvent(event, parent);
                default -> null;
            };
            if (change != null)
            {
                callback.vaultChanged(change);
            }
        }
    }

    private Change<?> processFileCreationEvent(
        DirectoryChangeEvent event, FileSystemFolder parent)
    {
        var eventAbsolutePath = event.path();
        if (event.isDirectory() && !isHidden(eventAbsolutePath))
        {
            var folder = parent.addFolder(folderName(eventAbsolutePath));
            LOGGER.debug("Detected new folder: {}", folder);
            try
            {
                walkFileTree(eventAbsolutePath, new VaultBuilder(folder, eventAbsolutePath));
                return creation(folder, Folder.class);
            }
            catch (IOException e)
            {
                LOGGER.warn("Error while processing file tree", e);
            }
        }
        else if (isDocument(eventAbsolutePath))
        {
            var document = newDocumentFromAbsolutePath(eventAbsolutePath);
            LOGGER.debug("Detected new document: {}", document);
            parent.addDocument(document);
            return creation(document, Document.class);
        }
        return null;
    }

    private Change<?> processFileModificationEvent(
        DirectoryChangeEvent event, FileSystemFolder parent)
    {
        var eventAbsolutePath = event.path();
        if (isDocument(eventAbsolutePath))
        {
            var document = newDocumentFromAbsolutePath(eventAbsolutePath);
            LOGGER.debug("Detected changes to document {}.", document);
            parent.addDocument(document);
            return modification(document, Document.class);
        }
        return null;
    }

    private Change<?> processFileDeletionEvent(
        DirectoryChangeEvent event, FileSystemFolder parent)
    {
        var eventAbsolutePath = event.path();
        if (isDocument(eventAbsolutePath))
        {
            var name = documentName(eventAbsolutePath);
            return parent.document(name).map(document ->
            {
                LOGGER.debug("Document deleted: {}", name);
                parent.removeDocument(name);
                return deletion(document, Document.class);
            }).orElse(null);
        }
        else
        {
            var name = folderName(eventAbsolutePath);
            return parent.folder(name).map(folder ->
            {
                LOGGER.debug("Folder deleted: {}", name);
                parent.removeFolder(name);
                return deletion(folder, Folder.class);
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

            var directory = relativePath.getName(i);
            if (isHidden(directory))
            {
                LOGGER.trace("Hidden directory. Doing nothing: {}", relativePath);
                return null;
            }
            var subfolder = folder.folder(folderName(directory)).orElse(null);
            if (subfolder == null)
            {
                LOGGER.trace("Couldn't find subfolder '{}' in folder '{}'. Doing nothing.",
                    directory, folder
                );
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
            var lastModified = getLastModifiedTime(absolutePath).toMillis();
            return newDocument(documentName(absolutePath), lastModified,
                readAllLines(absolutePath)
            );
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not read file " + absolutePath, e);
        }
    }

    private String folderName(Path absolutePath)
    {
        return normalize(absolutePath.getFileName().toString(), NFC);
    }

    private String documentName(Path absolutePath)
    {
        var fileName = absolutePath.getFileName().toString();
        var extensionIndex = fileName.lastIndexOf('.');
        var endIndex = extensionIndex != -1 ? extensionIndex : fileName.length();
        return normalize(fileName.substring(0, endIndex), NFC);
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
        LOGGER.trace("Resolved absolute path for document: {}", path);
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
                LOGGER.trace("Skipping directory {}", directory);
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
                    directory, exception
                );
            }
            if (!root.equals(directory))
            {
                currentFolder = currentFolder.parent();
            }
            return super.postVisitDirectory(directory, exception);
        }
    }
}

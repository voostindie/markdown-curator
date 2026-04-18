package nl.ulso.curator.vault;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.Change;
import nl.ulso.curator.change.ExternalChangeHandler;
import nl.ulso.curator.statistics.MeasurementCollector;
import nl.ulso.curator.statistics.MeasurementTracker;
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
import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.vault.DirectoryChangeEventHandler.DIRECTORY_CHANGE_EVENT_HANDLERS;
import static nl.ulso.curator.vault.DirectoryChangeEventHandler.FileSystemItemType;
import static nl.ulso.curator.vault.Document.newDocument;
import static org.slf4j.LoggerFactory.getLogger;

/// [Vault] implementation on top of the (default) filesystem.
///
/// On creation, it uses a [FileVisitor] to process all folders and documents and pull them in
/// memory. From then on it watches all folders and subfolders for changes using the file system's
/// [WatchService].
@Singleton
final class FileSystemVault
    extends FileSystemFolder
    implements Vault, DocumentPathResolver, ExternalChangeHandler, MeasurementTracker
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
        this.callback = _ -> {}; // By default, do nothing.
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
    public void process(Change<?> change)
    {
        callback.vaultChanged(change);
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
        LOGGER.info("Watching '{}' for changes.", absolutePath);
        watcher.watch();
    }

    @Override
    public void collectMeasurements(MeasurementCollector collector)
    {
        var statistics = ElementCounter.countFoldersAndDocuments(this);
        collector.total(Folder.class, statistics.folders());
        collector.total(Document.class, statistics.documents());
    }

    private void processFileSystemEvent(DirectoryChangeEvent event)
    {
        var eventAbsolutePath = event.path();
        LOGGER.trace("Change detected: {}", eventAbsolutePath);
        var parent = resolveParentFolder(eventAbsolutePath);
        if (parent == null)
        {
            LOGGER.trace(
                "Ignoring event for path '{}', parent folder not found.",
                eventAbsolutePath
            );
            return;
        }
        var item = event.isDirectory() ? FileSystemItemType.DIRECTORY : FileSystemItemType.FILE;
        if (item == FileSystemItemType.FILE && !isDocument(eventAbsolutePath))
        {
            LOGGER.trace("Ignoring event for file '{}', not a document", eventAbsolutePath);
            return;
        }
        var eventHandler = DIRECTORY_CHANGE_EVENT_HANDLERS.get(item).get(event.eventType());
        if (eventHandler == null)
        {
            LOGGER.trace(
                "Ignoring event with unsupported type '{}' for path '{}'",
                event.eventType(), eventAbsolutePath
            );
            return;
        }
        eventHandler.handle(event, parent, callback);
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
                LOGGER.trace("Hidden directory '{}'. Skipping.", relativePath);
                return null;
            }
            var subfolder = folder.folder(folderName(directory)).orElse(null);
            if (subfolder == null)
            {
                LOGGER.trace("Couldn't find subfolder '{}' in folder '{}'. Skipping.",
                    directory, folder
                );
                return null;
            }
            folder = subfolder;
        }
        return (FileSystemFolder) folder;
    }

    static boolean isHidden(Path directory)
    {
        return directory.getFileName().toString().startsWith(".");
    }

    static boolean isDocument(Path file)
    {
        return file.getFileName().toString().endsWith(".md");
    }

    static Document newDocumentFromAbsolutePath(Path absolutePath)
    {
        try
        {
            return newDocument(
                documentName(absolutePath),
                getLastModifiedTime(absolutePath).toMillis(),
                readAllLines(absolutePath)
            );
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not read file " + absolutePath, e);
        }
    }

    /// Returns the folder name for the given absolute path; the path is expected to represent a
    /// directory.
    static String folderName(Path absolutePath)
    {
        return normalize(absolutePath.getFileName().toString(), NFC);
    }

    /// Returns the document name for the given absolute path; the path is expected to represent a
    /// file.
    static String documentName(Path absolutePath)
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
        LOGGER.trace("Resolved absolute path for document '{}'.", path);
        return path;
    }

    static class VaultBuilder
        extends SimpleFileVisitor<Path>
    {
        private final Path root;
        private FileSystemFolder currentFolder;
        private final VaultChangedCallback vaultChangedCallback;

        VaultBuilder(
            FileSystemFolder targetFolder, Path absolutePath, VaultChangedCallback callback)
        {
            root = absolutePath;
            currentFolder = targetFolder;
            vaultChangedCallback = callback;
        }

        private VaultBuilder(FileSystemFolder targetFolder, Path absolutePath)
        {
            this(targetFolder, absolutePath, _ -> {});
        }

        @Override
        public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
            throws IOException
        {
            if (isHidden(directory))
            {
                LOGGER.trace("Skipping directory '{}'.", directory);
                return FileVisitResult.SKIP_SUBTREE;
            }
            if (!root.equals(directory))
            {
                currentFolder = currentFolder.addFolder(folderName(directory));
                vaultChangedCallback.vaultChanged(create(currentFolder, Folder.class));
            }
            return super.preVisitDirectory(directory, attributes);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
            throws IOException
        {
            if (isDocument(file))
            {
                var document = newDocumentFromAbsolutePath(file);
                currentFolder.addDocument(document);
                vaultChangedCallback.vaultChanged(create(document, Document.class));
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
                    "Traversing '{}' threw an exception; this might lead to unexpected behavior.",
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

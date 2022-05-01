package nl.ulso.macu.curator;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import nl.ulso.macu.query.QueryCatalog;
import nl.ulso.macu.vault.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * System for testing. All testing is done in memory. All files in the "music" directory from this
 * project are copied into a memory-based file system first, after which a {@link FileSystemVault}
 * is initialized on top of this file system. This ensures that the file system stays intact. The
 * vault on disk is there to make it easy to maintain and use: just fire up Obsidian on top of it.
 * <p/>
 * This class is in the same package as the {@link CuratorTemplate} base class, so that its
 * package-local methods are available for testing.
 */
public class MusicCurator
        extends CuratorTemplate
{
    @Override
    protected FileSystemVault createVault()
            throws IOException
    {
        var sourceRoot = Paths.get("").toAbsolutePath().resolve("music");
        var configuration = Configuration.unix().toBuilder().build();
        var targetFileSystem = Jimfs.newFileSystem(configuration);
        var targetRoot = targetFileSystem.getPath("/music");
        Files.walkFileTree(sourceRoot, new RecursiveCopier(sourceRoot, targetRoot));
        return new FileSystemVault(targetRoot, targetFileSystem.newWatchService());
    }

    @Override
    protected void registerQueries(QueryCatalog catalog, Vault vault)
    {
        catalog.register(new AlbumsQuery(vault));
        catalog.register(new RecordingsQuery(vault));
        catalog.register(new MembersQuery(vault));
    }

    String reload(Document document)
            throws IOException
    {
        var path = vault().resolveAbsolutePath(document);
        return Files.readString(path).trim();
    }

    private static class RecursiveCopier
            extends SimpleFileVisitor<Path>
    {
        private final Path sourceRoot;
        private final Path targetRoot;

        public RecursiveCopier(Path sourceRoot, Path targetRoot)
        {
            this.sourceRoot = sourceRoot;
            this.targetRoot = targetRoot;
        }

        @Override
        public FileVisitResult preVisitDirectory(
                Path sourceDirectory,
                BasicFileAttributes attributes)
                throws IOException
        {
            Path targetPath = resolveTargetPath(sourceDirectory);
            Files.createDirectory(targetPath);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attributes)
                throws IOException
        {
            Files.copy(sourceFile, resolveTargetPath(sourceFile));
            return FileVisitResult.CONTINUE;
        }

        /**
         * Because the source and target roots are in different file system implementations,
         * resolving the target path from a source path is a bit cumbersome. "resolve" doesn't work
         * across file systems.
         *
         * @param sourcePath source path to resolve in the target.
         * @return The corresponding path in the target root.
         */
        private Path resolveTargetPath(Path sourcePath)
        {
            var targetPath = targetRoot;
            for (var path : sourceRoot.relativize(sourcePath))
            {
                targetPath = targetPath.resolve(path.toString());
            }
            return targetPath;
        }
    }
}

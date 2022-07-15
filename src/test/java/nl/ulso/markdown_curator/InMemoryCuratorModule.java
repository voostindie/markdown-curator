package nl.ulso.markdown_curator;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.inject.Provides;
import nl.ulso.markdown_curator.vault.FileSystemVault;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

abstract class InMemoryCuratorModule
        extends CuratorModule
{
    protected Path copyVaultToMemory(Path sourceRoot, String targetPath)
    {
        var configuration = Configuration.unix().toBuilder().build();
        var targetFileSystem = Jimfs.newFileSystem(configuration);
        var targetRoot = targetFileSystem.getPath(targetPath);
        try
        {
            Files.walkFileTree(sourceRoot, new RecursiveCopier(sourceRoot, targetRoot));
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Couldn't copy filesystem into memory", e);
        }
        return targetRoot;
    }

    @Provides
    public WatchService watchService(@VaultPath Path vaultPath)
            throws IOException
    {
        return vaultPath.getFileSystem().newWatchService();
    }

    /**
     * Recursively copies all files and directories. In this specific case the source is on an
     * actual filesystem, while the target is in memory.
     */
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

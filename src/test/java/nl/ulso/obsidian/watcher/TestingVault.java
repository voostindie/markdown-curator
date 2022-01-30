package nl.ulso.obsidian.watcher;

import nl.ulso.obsidian.watcher.vault.FileSystemVault;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class TestingVault
        extends FileSystemVault
{
    public TestingVault()
            throws IOException
    {
        super(resolveVaultPath());
    }

    private static Path resolveVaultPath()
    {
        return FileSystems.getDefault().getPath(System.getProperty("user.dir"))
                .resolve(Path.of("src", "test", "resources", "vault"));
    }
}

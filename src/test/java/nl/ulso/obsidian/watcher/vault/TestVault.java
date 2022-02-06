package nl.ulso.obsidian.watcher.vault;

import java.io.IOException;
import java.nio.file.Path;

public class TestVault
        extends FileSystemVault
{
    static final String ROOT = "/vault";

    TestVault(Path absolutePath)
            throws IOException
    {
        super(absolutePath);
    }
}

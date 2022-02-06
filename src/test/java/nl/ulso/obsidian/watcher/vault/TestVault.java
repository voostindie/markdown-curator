package nl.ulso.obsidian.watcher.vault;

import java.io.IOException;

public class TestVault
        extends FileSystemVault
{
    static final String ROOT = "/vault";

    public TestVault()
            throws IOException
    {
        super(getFileSystem().getPath(ROOT));
    }
}

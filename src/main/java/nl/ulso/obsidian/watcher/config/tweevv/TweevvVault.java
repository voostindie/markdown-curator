package nl.ulso.obsidian.watcher.config.tweevv;

import nl.ulso.obsidian.watcher.vault.FileSystemVault;

import java.io.IOException;
import java.nio.file.Path;

public class TweevvVault
        extends FileSystemVault
{
    public TweevvVault()
            throws IOException
    {
        super(Path.of("/Users", "vincent", "Notes", "TweeVV"));
    }
}

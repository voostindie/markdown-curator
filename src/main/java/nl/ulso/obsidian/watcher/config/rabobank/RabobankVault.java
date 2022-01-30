package nl.ulso.obsidian.watcher.config.rabobank;

import nl.ulso.obsidian.watcher.vault.FileSystemVault;

import java.io.IOException;
import java.nio.file.Path;

public class RabobankVault
        extends FileSystemVault
{
    public RabobankVault()
            throws IOException
    {
        super(Path.of("/Users", "vincent", "Notes", "Rabobank"));
    }
}

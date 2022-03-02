package nl.ulso.obsidian.watcher.config.tweevv;

import nl.ulso.obsidian.watcher.System;
import nl.ulso.obsidian.watcher.vault.FileSystemVault;
import nl.ulso.obsidian.watcher.vault.Vault;

import java.io.IOException;
import java.nio.file.Path;

public class TweevvSystem
        implements System
{
    private final Vault vault;

    public TweevvSystem()
            throws IOException
    {
        this.vault = new FileSystemVault(Path.of("/Users", "vincent", "Notes", "TweeVV"));
    }

    @Override
    public Vault vault()
    {
        return vault;
    }
}

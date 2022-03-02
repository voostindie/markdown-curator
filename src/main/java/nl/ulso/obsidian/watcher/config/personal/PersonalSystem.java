package nl.ulso.obsidian.watcher.config.personal;

import nl.ulso.obsidian.watcher.System;
import nl.ulso.obsidian.watcher.vault.FileSystemVault;
import nl.ulso.obsidian.watcher.vault.Vault;

import java.io.IOException;
import java.nio.file.Path;

public class PersonalSystem
        implements System
{
    private final Vault vault;

    public PersonalSystem()
            throws IOException
    {
        this.vault = new FileSystemVault(Path.of("/Users", "vincent", "Notes", "Personal"));
    }

    @Override
    public Vault vault()
    {
        return vault;
    }
}

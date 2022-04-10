package nl.ulso.macu.config.rabobank;

import nl.ulso.macu.System;
import nl.ulso.macu.vault.FileSystemVault;
import nl.ulso.macu.vault.Vault;

import java.io.IOException;
import java.nio.file.Path;

public class RabobankSystem
        implements System
{
    private final Vault vault;

    public RabobankSystem()
            throws IOException
    {
        vault = new FileSystemVault(Path.of("/Users", "vincent", "Notes", "Rabobank"));
    }

    public Vault vault()
    {
        return vault;
    }
}

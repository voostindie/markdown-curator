package nl.ulso.macu.config.personal;

import nl.ulso.macu.System;
import nl.ulso.macu.vault.FileSystemVault;
import nl.ulso.macu.vault.Vault;

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

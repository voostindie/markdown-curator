package nl.ulso.macu.system.rabobank;

import nl.ulso.macu.System;
import nl.ulso.macu.query.QueryCatalog;
import nl.ulso.macu.vault.FileSystemVault;
import nl.ulso.macu.vault.Vault;

import java.io.IOException;
import java.nio.file.Path;

public class Rabobank
        implements System
{
    private final Vault vault;

    public Rabobank()
            throws IOException
    {
        vault = new FileSystemVault(Path.of("/Users", "vincent", "Notes", "Rabobank"));
    }

    public Vault vault()
    {
        return vault;
    }

    @Override
    public QueryCatalog queryCatalog()
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void run()
            throws IOException, InterruptedException
    {
        vault.watchForChanges();
    }
}

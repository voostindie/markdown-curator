package nl.ulso.macu.system.tweevv;

import nl.ulso.macu.System;
import nl.ulso.macu.query.QueryCatalog;
import nl.ulso.macu.vault.FileSystemVault;
import nl.ulso.macu.vault.Vault;

import java.io.IOException;
import java.nio.file.Path;

public class Tweevv
        implements System
{
    private final Vault vault;

    public Tweevv()
            throws IOException
    {
        this.vault = new FileSystemVault(Path.of("/Users", "vincent", "Notes", "TweeVV"));
    }

    @Override
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

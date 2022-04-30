package nl.ulso.macu.system.tweevv;

import nl.ulso.macu.system.SystemTemplate;
import nl.ulso.macu.query.QueryCatalog;
import nl.ulso.macu.vault.FileSystemVault;
import nl.ulso.macu.vault.Vault;

import java.io.IOException;
import java.nio.file.Path;

public class Tweevv
        extends SystemTemplate
{
    @Override
    protected FileSystemVault createVault()
            throws IOException
    {
        return createVaultForPathInUserHome("Notes", "TweeVV");
    }

    @Override
    protected void registerQueries(QueryCatalog catalog, Vault vault)
    {

    }
}

package nl.ulso.macu.curator.tweevv;

import nl.ulso.macu.curator.CuratorTemplate;
import nl.ulso.macu.query.QueryCatalog;
import nl.ulso.macu.vault.FileSystemVault;
import nl.ulso.macu.vault.Vault;

import java.io.IOException;

public class TweevvNotesCurator
        extends CuratorTemplate
{
    @Override
    protected FileSystemVault createVault()
            throws IOException
    {
        return createVaultForPathInUserHome("Notes", "TweeVV");
    }

    @Override
    protected void registerQueries(QueryCatalog catalog, FileSystemVault vault)
    {

    }
}

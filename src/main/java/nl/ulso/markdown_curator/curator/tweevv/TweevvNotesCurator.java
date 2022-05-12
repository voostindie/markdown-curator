package nl.ulso.markdown_curator.curator.tweevv;

import nl.ulso.markdown_curator.CuratorTemplate;
import nl.ulso.markdown_curator.curator.common.omnifocus.OmniFocusQuery;
import nl.ulso.markdown_curator.query.QueryCatalog;
import nl.ulso.markdown_curator.vault.FileSystemVault;
import nl.ulso.markdown_curator.vault.Vault;

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
    protected void registerQueries(QueryCatalog catalog, Vault vault)
    {
        catalog.register(new OmniFocusQuery(vault));
    }
}

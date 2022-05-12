package nl.ulso.markdown_curator.curator.personal;

import nl.ulso.markdown_curator.CuratorTemplate;
import nl.ulso.markdown_curator.query.QueryCatalog;
import nl.ulso.markdown_curator.vault.FileSystemVault;
import nl.ulso.markdown_curator.vault.Vault;

import java.io.IOException;

public class PersonalNotesCurator
        extends CuratorTemplate
{
    @Override
    protected FileSystemVault createVault()
            throws IOException
    {
        return createVaultForPathInUserHome("Notes", "Personal");
    }

    @Override
    protected void registerQueries(QueryCatalog catalog, Vault vault)
    {
    }
}

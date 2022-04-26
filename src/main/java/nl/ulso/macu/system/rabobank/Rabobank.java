package nl.ulso.macu.system.rabobank;

import nl.ulso.macu.SystemTemplate;
import nl.ulso.macu.query.QueryCatalog;
import nl.ulso.macu.vault.FileSystemVault;
import nl.ulso.macu.vault.Vault;

import java.io.IOException;
import java.nio.file.Path;

public class Rabobank
        extends SystemTemplate
{
    @Override
    protected Vault createVault()
            throws IOException
    {
        return new FileSystemVault(
                Path.of(java.lang.System.getProperty("user.home"), "Notes", "Rabobank"));
    }

    @Override
    protected void registerQueries(QueryCatalog catalog, Vault vault)
    {

    }
}

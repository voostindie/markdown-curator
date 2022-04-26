package nl.ulso.macu;

import nl.ulso.macu.query.InMemoryQueryCatalog;
import nl.ulso.macu.query.QueryCatalog;
import nl.ulso.macu.vault.Vault;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

public abstract class SystemTemplate
        implements System
{
    private static final Logger LOGGER = getLogger(SystemTemplate.class);

    private final Vault vault;
    private final QueryCatalog queryCatalog;

    public SystemTemplate()
    {
        try
        {
            this.vault = createVault();
        }
        catch (IOException e)
        {
            LOGGER.error("Couldn't create vault. Reason: {}", e.toString());
            throw new RuntimeException(e);
        }
        this.queryCatalog = new InMemoryQueryCatalog();
        registerQueries(queryCatalog, vault);
    }

    protected abstract Vault createVault()
            throws IOException;

    protected abstract void registerQueries(QueryCatalog catalog, Vault vault);

    @Override
    public final void run()
            throws IOException, InterruptedException
    {
        vault.watchForChanges();
    }

    public final Vault vault()
    {
        return vault;
    }

    public final QueryCatalog queryCatalog()
    {
        return queryCatalog;
    }
}

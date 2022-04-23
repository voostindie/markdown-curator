package nl.ulso.macu;

import nl.ulso.macu.query.QueryCatalog;
import nl.ulso.macu.vault.Vault;

import java.io.IOException;

/**
 * Defines a single configuration of a vault, a query catalog and anything else the system might
 * encapsulate.
 */
public interface System
{
    Vault vault();

    QueryCatalog queryCatalog();

    /**
     * Runs this system by watching the vault for changing and executing commands as a result of
     * these changes. This method blocks.
     */
    void run()
            throws IOException, InterruptedException;
}

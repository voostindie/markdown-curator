package nl.ulso.macu.vault;

import java.io.IOException;
import java.util.Collection;

public interface Vault
        extends Folder
{
    Collection<QueryBlock> findAllQueryBlocks();

    /**
     * Watches the vault for changes and acts accordingly when it does. This method blocks:
     * it simply waits for changes. If no changes come in, the thread it's running in is blocked.
     */
    void watchForChanges()
            throws IOException, InterruptedException;
}

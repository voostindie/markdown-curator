package nl.ulso.obsidian.watcher.vault;

import java.io.IOException;
import java.util.Map;

public interface Vault
        extends Folder
{
    Map<Query, Location> findAllQueries();

    /**
     * Watches the vault for changes and acts accordingly when it does. This method blocks:
     * it simply waits for changes. If no changes come in, the thread it's running in is blocked.
     */
    void watchForChanges()
            throws IOException, InterruptedException;
}

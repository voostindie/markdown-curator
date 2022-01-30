package nl.ulso.obsidian.watcher.vault;

import java.io.IOException;

public interface Vault
{
    void accept(Visitor visitor);

    /**
     * Watches the vault for changes and acts accordingly when it does. This method blocks:
     * it simply waits for changes. If no changes come in, the thread it's running in is blocked.
     */
    void watchForChanges()
            throws IOException, InterruptedException;
}

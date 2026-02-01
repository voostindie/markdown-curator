package nl.ulso.curator.vault;

import nl.ulso.curator.Change;

/**
 * Triggers a change event to the vault to have it execute all queries, even though there were no
 * changes detected in the vault itself.
 */
public interface VaultRefresher
{
    void triggerRefresh(Change<?> change);
}

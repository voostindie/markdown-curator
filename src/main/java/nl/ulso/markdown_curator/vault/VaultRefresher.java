package nl.ulso.markdown_curator.vault;

/**
 * Triggers a change event to the vault to have it execute all queries, even though there were no
 * changes detected in the vault itself.
 */
public interface VaultRefresher
{
    void triggerRefresh();
}

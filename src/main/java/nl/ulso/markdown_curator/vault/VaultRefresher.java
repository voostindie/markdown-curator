package nl.ulso.markdown_curator.vault;

import nl.ulso.markdown_curator.vault.event.ExternalChange;

/**
 * Triggers a change event to the vault of type  {@link ExternalChange} to have it execute all
 * queries, even though there were no changes in the vault itself.
 */
public interface VaultRefresher
{
    void triggerRefresh();
}

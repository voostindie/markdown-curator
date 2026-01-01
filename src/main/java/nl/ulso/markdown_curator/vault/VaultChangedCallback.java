package nl.ulso.markdown_curator.vault;

import nl.ulso.markdown_curator.Change;

/**
 * Callback that is triggered whenever a meaningful change to the vault has been detected.
 */
public interface VaultChangedCallback
{
    void vaultChanged(Change<?> change);
}

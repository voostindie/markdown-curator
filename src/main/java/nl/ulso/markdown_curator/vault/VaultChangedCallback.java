package nl.ulso.markdown_curator.vault;

import nl.ulso.markdown_curator.Change;
import nl.ulso.markdown_curator.Changelog;

/**
 * Callback that is triggered whenever a meaningful change to the vault has been detected.
 */
public interface VaultChangedCallback
{
    Changelog vaultChanged(Change<?> change);
}

package nl.ulso.curator.vault;

import nl.ulso.curator.changelog.Change;

/// Callback that is triggered whenever a meaningful change to the vault has been detected.
public interface VaultChangedCallback
{
    void vaultChanged(Change<?> change);
}

package nl.ulso.curator.main;

import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.vault.Vault;

import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Changelog.changelogFor;

public class VaultTestSupport
{
    /// Takes a vault and creates a changelog with the initial vault state.
    /// Run this through your change processor(s) to apply the initial state.
    public static Changelog initializeVault(Vault vault)
    {
        return new VaultInitializer(vault).apply(changelogFor(create(vault, Vault.class)));
    }

}

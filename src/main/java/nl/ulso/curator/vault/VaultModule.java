package nl.ulso.curator.vault;

import dagger.Binds;
import dagger.Module;
import nl.ulso.curator.change.ExternalChangeHandler;

@Module
public abstract class VaultModule
{
    @Binds
    abstract Vault bindVault(FileSystemVault vault);

    @Binds
    abstract DocumentPathResolver bindDocumentPathResolver(FileSystemVault vault);

    @Binds
    abstract ExternalChangeHandler bindExternalChangeHandler(FileSystemVault vault);
}

package nl.ulso.curator.vault;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.change.ExternalChangeHandler;
import nl.ulso.curator.statistics.MeasurementTracker;
import nl.ulso.curator.statistics.StatisticsModule;

@Module(includes = {StatisticsModule.class})
public abstract class VaultModule
{
    @Binds
    abstract Vault bindVault(FileSystemVault vault);

    @Binds
    abstract DocumentPathResolver bindDocumentPathResolver(FileSystemVault vault);

    @Binds
    abstract ExternalChangeHandler bindExternalChangeHandler(FileSystemVault vault);

    @Binds
    @IntoSet
    abstract MeasurementTracker bindMeasurementTracker(FileSystemVault vault);
}

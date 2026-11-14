package nl.ulso.curator.main;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.Curator;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.query.QueryModule;
import nl.ulso.curator.statistics.MeasurementTracker;
import nl.ulso.curator.vault.VaultModule;

import java.util.Locale;

@Module(includes = {QueryModule.class, VaultModule.class})
public abstract class MainModule
{
    @Binds
    abstract Curator bindCurator(DefaultCurator curator);

    /// Provides the locale as currently configured in the configuration document. The locale can
    /// change at runtime, so classes that need it must inject a `Provider<Locale>` and ask for the
    /// locale every time they need it.
    @Provides
    static Locale provideLocale(CuratorSettings settings)
    {
        return settings.locale();
    }

    @Binds
    abstract ChangeProcessorOrchestrator bindChangeProcessorOrchestrator(
        DefaultChangeProcessorOrchestrator orchestrator);

    @Binds
    abstract QueryOrchestrator bindQueryOrchestrator(DefaultQueryOrchestrator orchestrator);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindVaultInitializer(VaultInitializer vaultInitializer);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindVaultReloader(VaultReloader vaultReloader);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindConfigurationLoader(ConfigurationLoader configurationLoader);

    @Binds
    @IntoSet
    abstract Query bindExtensionsQuery(ExtensionsQuery extensionsQuery);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindFrontMatterProcessor(FrontMatterRepository frontMatterRepository);

    @Binds
    abstract FrontMatterCollector bindFrontMatterCollector(
        FrontMatterRepository repository);

    @Binds
    abstract FrontMatterRewriteResolver bindFrontMatterRewriteResolver(
        FrontMatterRepository repository);

    @Binds
    @IntoSet
    abstract MeasurementTracker bindFrontMatterMeasurements(FrontMatterRepository frontMatterRepository);

    @Binds
    @IntoSet
    abstract Query bindChangeProcessorGraphQuery(ChangeProcessorGraphQuery changeProcessorGraphQuery);
}

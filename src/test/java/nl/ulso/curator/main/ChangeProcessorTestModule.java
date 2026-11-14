package nl.ulso.curator.main;

import dagger.*;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.change.ExternalChangeHandler;
import nl.ulso.curator.extension.Contribution;
import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.curator.extension.ServiceRegistration;
import nl.ulso.curator.query.DynamicQueryCatalog;
import nl.ulso.curator.statistics.StatisticsModule;
import nl.ulso.curator.vault.Vault;
import nl.ulso.curator.vault.VaultStub;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/// Basic test module for change processors that uses a vault stub.
@Module(includes = {StatisticsModule.class})
public abstract class ChangeProcessorTestModule
{
    @Multibinds
    abstract Set<ChangeProcessor> bindAllAvailableChangeProcessors();

    @Multibinds
    abstract Map<String, ExtensionFactory> bindAllAvailableExtensionFactories();

    @Provides
    static Locale provideLocale(CuratorSettings settings)
    {
        return settings.locale();
    }

    /// Changes from outside the system are ignored in tests.
    @Provides
    static ExternalChangeHandler provideExternalChangeHandler()
    {
        return _ -> {
            // Ignore.
        };
    }

    @Multibinds
    abstract Set<ServiceRegistration> bindAllCuratorServices();

    @Multibinds
    abstract Set<Contribution> bindAllCuratorContributions();

    /// Change processor tests don't use the query module, but the extension registry needs a
    /// catalog.
    @Provides
    @Singleton
    static DynamicQueryCatalog provideEmptyQueryCatalog()
    {
        return new DynamicQueryCatalogStub();
    }

    @Provides
    @Singleton
    static VaultStub provideVaultStub()
    {
        return new VaultStub();
    }

    @Binds
    abstract Vault bindVault(VaultStub vaultStub);

    @Binds
    abstract ChangeProcessorOrchestrator bindChangeProcessorOrchestrator(
        DefaultChangeProcessorOrchestrator orchestrator);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindVaultInitializer(VaultInitializer vaultInitializer);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindFrontMatterProcessor(FrontMatterRepository frontMatterRepository);

    @Binds
    abstract FrontMatterCollector bindFrontMatterCollector(
        FrontMatterRepository repository);

    @Binds
    abstract FrontMatterRewriteResolver bindFrontMatterRewriteResolver(
        FrontMatterRepository repository);
}

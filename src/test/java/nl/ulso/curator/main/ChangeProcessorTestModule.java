package nl.ulso.curator.main;

import dagger.*;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.curator.query.DynamicQueryCatalog;
import nl.ulso.curator.statistics.StatisticsModule;
import nl.ulso.curator.vault.Vault;
import nl.ulso.curator.vault.VaultStub;

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

    /// Change processor tests don't need queries, but the extension registry needs a catalog.
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

package nl.ulso.curator.main;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import nl.ulso.curator.Curator;
import nl.ulso.curator.FrontMatterCollector;
import nl.ulso.curator.changelog.ChangeProcessor;
import nl.ulso.curator.query.QueryModule;
import nl.ulso.curator.vault.VaultModule;

@Module(includes = {QueryModule.class, VaultModule.class})
public abstract class MainModule
{
    @Binds
    abstract Curator bindCurator(CuratorImpl curator);

    @Binds
    abstract ChangeProcessorOrchestrator bindDataModelOrchestrator(
        ChangeProcessorOrchestratorImpl orchestrator);

    @Binds
    abstract QueryOrchestrator bindQueryOrchestrator(QueryOrchestratorImpl orchestrator);

    @Binds
    @IntoSet
    abstract ChangeProcessor bindVaultReloader(VaultReloader vaultReloader);

    @Binds
    abstract FrontMatterCollector bindFrontMatterCollector(
        FrontMatterRepository repository);

    @Binds
    abstract FrontMatterRewriteResolver bindFrontMatterRewriteResolver(
        FrontMatterRepository repository);
}

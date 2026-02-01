package nl.ulso.curator;

import dagger.*;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import jakarta.inject.Named;
import nl.ulso.curator.query.*;
import nl.ulso.curator.query.builtin.*;
import nl.ulso.curator.vault.*;

import java.nio.file.WatchService;
import java.util.Locale;
import java.util.Set;

/// Basic Dagger module for curators. Every new curator module must include this module.
///
/// Curators must do the following things:
///
/// - Provide a `Path`. This is the path on disk to the vault (repository) with Markdown files.
/// - (Optional): provide a [java.util.Locale]. This will be used for translations in normal output.
/// The default is English. (Instructions and error messages are all in English, hard-coded.)
/// - (Optional): register custom [ChangeProcessor]s: Create an abstract method for every concrete
/// implementation that binds it to the `ChangeProcessor` interface. Note that every change
/// processor must be a singleton.
/// - (Optional): register custom [Query]s. Create an abstract method for every concrete
/// implementation that binds it to the `Query` interface.
/// </ol>
///
/// Curator modules can use the full power of Dagger to configure themselves however they want.
@Module
public abstract class CuratorModule
{
    @Multibinds
    abstract Set<Query> queries();

    @Multibinds
    abstract Set<ChangeProcessor> changeProcessors();

    @BindsOptionalOf
    abstract WatchService optionalWatchService();

    @BindsOptionalOf
    abstract Locale optionalLocale();

    @Binds
    abstract Vault bindVault(FileSystemVault vault);

    @Binds
    abstract DocumentPathResolver bindDocumentPathResolver(FileSystemVault vault);

    @Binds
    abstract VaultRefresher bindVaultRefresher(FileSystemVault vault);

    @Binds
    abstract QueryCatalog bindQueryCatalog(InMemoryQueryCatalog queryCatalog);

    @Binds
    abstract ChangeProcessorOrchestrator bindDataModelOrchestrator(
        ChangeProcessorOrchestratorImpl orchestrator);

    @Binds
    abstract QueryOrchestrator bindQueryOrchestrator(QueryOrchestratorImpl orchestrator);

    @Binds
    abstract GeneralMessages bindGeneralMessages(ResourceBundledGeneralMessages generalMessages);

    @BindsOptionalOf
    @Named("watchdoc")
    abstract String watchDocumentName();

    @Binds
    @IntoSet
    abstract ChangeProcessor bindVaultReloader(VaultReloader vaultReloader);

    @Binds
    abstract FrontMatterUpdateCollector bindFrontMatterCollector(
        FrontMatterCollector collector);

    @Binds
    abstract FrontMatterRewriteResolver bindFrontMatterRewriteResolver(
        FrontMatterCollector collector);

    @Binds
    @IntoSet
    abstract Query bindListQuery(ListQuery listQuery);

    @Binds
    @IntoSet
    abstract Query bindTableQuery(TableQuery tableQuery);

    @Binds
    @IntoSet
    abstract Query bindTableOfContentsQuery(TableOfContentsQuery tableOfContentsQuery);
}

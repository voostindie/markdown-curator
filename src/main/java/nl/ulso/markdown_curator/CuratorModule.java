package nl.ulso.markdown_curator;

import dagger.*;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.query.builtin.*;
import nl.ulso.markdown_curator.vault.*;

import java.nio.file.WatchService;
import java.util.Locale;
import java.util.Set;

/**
 * Basic Dagger module for curators. Every new curator module must include this module.
 * <p/>
 * Curators must do the following things:
 * <p/>
 * <ol>
 *     <li>Provide a {@code Path}. This is the path on disk to the vault (repository) with Markdown
 *     files.</li>
 *     <li>(Optional): provide a {@link java.util.Locale}. This will be used for translations in
 *     normal output. The default is English. (Instructions and error messages are all in
 *     English, hard-coded.)</li>
 *     <li>(Optional): register custom {@link DataModel}s: Create an abstract method for every
 *     concrete implementation that binds it to the {@code DataModel} interface. Note that
 *     every data model must be a singleton.</li>
 *     <li>(Optional): register custom {@link Query}s. Create an abstract method for every
 *     concrete implementation that binds it to the {@code Query} interface.</li>
 * </ol>
 * <p/>
 * Curator modules can use the full power of Dagger to configure themselves however they want.
 */
@Module
public abstract class CuratorModule
{
    @Multibinds
    abstract Set<Query> queries();

    @Multibinds
    abstract Set<DataModel> dataModels();

    @Multibinds
    @ExternalChangeObjectType
    abstract Set<Class<?>> externalChangeObjectTypes();

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
    abstract DataModelOrchestrator bindDataModelOrchestrator(
        DataModelOrchestratorImpl orchestrator);

    @Binds
    abstract GeneralMessages bindGeneralMessages(ResourceBundledGeneralMessages generalMessages);

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

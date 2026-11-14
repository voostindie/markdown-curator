package nl.ulso.curator;

import dagger.*;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import jakarta.inject.Named;
import nl.ulso.curator.addon.alfred.AlfredExtensionModule;
import nl.ulso.curator.addon.journal.JournalExtensionModule;
import nl.ulso.curator.addon.omnifocus.OmniFocusExtensionModule;
import nl.ulso.curator.addon.project.ProjectExtensionModule;
import nl.ulso.curator.addon.projectjournal.ProjectJournalExtensionModule;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.extension.Contribution;
import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.curator.extension.ServiceRegistration;
import nl.ulso.curator.main.MainModule;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.query.QueryResultFormatter;

import java.nio.file.WatchService;
import java.util.Map;
import java.util.Set;

/// Basic Dagger module for curators. Every new curator module must include this module.
///
/// A curator module **must** do one thing: provide a `Path`. This is the path on disk to the vault
/// (repository) with Markdown files.
///
/// Optionally, a curator module *can*:
///
/// - Register custom [ChangeProcessor]s: Create an abstract method for every concrete
/// implementation that binds it to the `ChangeProcessor` interface. Note that every change
/// processor must be a singleton.
/// - Register custom [Query]s. Create an abstract method for every concrete implementation that
/// binds it to the `Query` interface.
/// - Register custom [QueryResultFormatter]s.
/// - Provide a [WatchService] to detect changes to the vault. The default implementation is
/// optimized for macOS. Other platforms are not tested.
/// - Provide the name of a "watch document". This is a file in the root of the vault that, when
/// changed, will trigger a re-run of all queries.
/// - Provide the name of a "configuration document" to enable extensions, and register custom
/// [ExtensionFactory]s. Extensions are sets of change processors, queries, query result formatters
/// and trackers that are loaded and unloaded at runtime, as configured in the first YAML code block
/// of the configuration document. See [ExtensionFactory]. All built-in extensions (`project`,
/// `journal`, `projectjournal`, `omnifocus` and `alfred`) are registered by this module; they only
/// have to be enabled in the configuration document.
/// The same code block holds the settings of the curator itself, like the `delay` before queries
/// run after a change, and the `locale` for translations in query output.
/// - Include other modules and their configurations to extend the functionality of the curator.
@Module(includes = {
    MainModule.class,
    ProjectExtensionModule.class,
    JournalExtensionModule.class,
    ProjectJournalExtensionModule.class,
    OmniFocusExtensionModule.class,
    AlfredExtensionModule.class
})
public abstract class CuratorModule
{
    /// Name of the key to bind the watch document to in the Dagger module, as a [String].
    public static final String WATCH_DOCUMENT_KEY = "watchdoc";

    /// Name of the key to bind the configuration document to in the Dagger module, as a [String].
    public static final String CONFIGURATION_DOCUMENT_KEY = "configdoc";

    /// To add a [ChangeProcessor], configure it so that it [Binds] [IntoSet] of [ChangeProcessor].
    @Multibinds
    abstract Set<ChangeProcessor> bindAllAvailableChangeProcessors();

    /// To add a [Query], configure it so that it [Binds] [IntoSet] of [Query].
    @Multibinds
    abstract Set<Query> bindAllAvailableQueries();

    /// To add a [QueryResultFormatter], configure it so that it [Binds] [IntoSet] of
    /// [QueryResultFormatter<?>].
    @Multibinds
    abstract Set<QueryResultFormatter<?>> bindAllAvailableQueryResultFormatters();
    
    /// To add an [ExtensionFactory], configure it so that it [Binds] [IntoMap] of
    /// [ExtensionFactory], with a [dagger.multibindings.StringKey] as the name of the extension.
    @Multibinds
    abstract Map<String, ExtensionFactory> bindAllAvailableExtensionFactories();

    /// To publish a service for extensions to require, bind a [ServiceRegistration] [IntoSet].
    @Multibinds
    abstract Set<ServiceRegistration> bindAllCuratorServices();

    /// To contribute to an extension point, bind a [Contribution] [IntoSet].
    @Multibinds
    abstract Set<Contribution> bindAllCuratorContributions();

    /// Bind a custom [WatchService]; the default is optimized for macOS.
    @BindsOptionalOf
    abstract WatchService bindOptionalWatchService();

    /// Bind a watch document; by default, there is none.
    @BindsOptionalOf
    @Named(WATCH_DOCUMENT_KEY)
    abstract String bindOptionalWatchDocumentName();

    /// Bind a configuration document; by default, there is none.
    @BindsOptionalOf
    @Named(CONFIGURATION_DOCUMENT_KEY)
    abstract String bindOptionalConfigurationDocumentName();
}

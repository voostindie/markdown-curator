package nl.ulso.curator;

import dagger.*;
import dagger.Module;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import jakarta.inject.Named;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.extension.Contribution;
import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.curator.extension.ServiceRegistration;
import nl.ulso.curator.main.MainModule;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.query.QueryResultFormatter;

import java.nio.file.WatchService;
import java.util.Locale;
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
/// - Reqister custom [QueryResultFormatter]s.
/// - Provide a [WatchService] to detect changes to the vault. The default implementation is
/// optimized for macOS. Other platforms are not tested.
/// - Provide a [Locale]. This will be used for translations in output. The default is English.
/// (Instructions and error messages are all in English, hard-coded.)
/// - Provide the name of a "watch document". This is a file in the root of the vault that, when
/// changed, will trigger a re-run of all queries.
/// - Register [ExtensionFactory]s, and provide the name of a "configuration document". Extensions
/// are sets of change processors, queries and trackers that are loaded and unloaded at runtime,
/// as configured in the front matter of the configuration document. See [ExtensionFactory].
/// - Include other modules and their configurations to extend the functionality of the curator.
@Module(includes = {MainModule.class})
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

    /// Bind a custom [Locale]; the default is English.
    @BindsOptionalOf
    abstract Locale bindOptionalLocale();

    /// Bind a watch document; by default, there is none.
    @BindsOptionalOf
    @Named(WATCH_DOCUMENT_KEY)
    abstract String bindOptionalWatchDocumentName();

    /// Bind a configuration document; by default, there is none.
    @BindsOptionalOf
    @Named(CONFIGURATION_DOCUMENT_KEY)
    abstract String bindOptionalConfigurationDocumentName();
}

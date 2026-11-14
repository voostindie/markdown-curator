package nl.ulso.curator.extension;

import nl.ulso.dictionary.Dictionary;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

/// Creates [Extension]s from configuration, and describes them, so that the curator can decide
/// whether and in what order to create them.
///
/// Register an implementation in the curator's Dagger module, keyed by the name under which the
/// extension can be enabled in the configuration document:
///
/// ```java
/// @Binds
/// @IntoMap
/// @StringKey("directory")
/// abstract ExtensionFactory bindDirectoryExtensionFactory(DirectoryExtensionFactory factory);
/// ```
///
/// Extensions are loaded in two phases:
///
/// 1. All enabled extensions are described: their settings are validated, the services they
/// require and provide are resolved, and their [Contribution]s are collected. Extensions whose
/// requirements can't be met are dropped.
/// 2. The remaining extensions are created, providers before consumers, through
/// [#create(Dictionary, ExtensionContext)].
///
/// @see nl.ulso.curator.CuratorModule#CONFIGURATION_DOCUMENT_KEY
public interface ExtensionFactory
{
    /// @return A short, one-line description of the extension, in Markdown.
    String description();

    /// @return Map of name-description pairs of configuration settings; each description is a
    /// Markdown one-liner. Any other setting in the configuration is rejected.
    default Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    /// @return The types of services this extension needs from the curator or other extensions;
    /// available through [ExtensionContext#service(Class)].
    default Set<Class<?>> requires()
    {
        return emptySet();
    }

    /// @return The types of services this extension provides; the extension must register exactly
    /// these through [Extension#services()].
    default Set<Class<?>> provides()
    {
        return emptySet();
    }

    /// Returns the contributions of this extension to [ExtensionPoint]s, typically owned by other
    /// extensions. This method is called before any extension is created.
    ///
    /// @param configuration the configuration of the extension.
    /// @return the contributions; empty by default.
    default List<Contribution> contributions(Dictionary configuration)
    {
        return emptyList();
    }

    /// Creates a new, fresh extension instance.
    ///
    /// @param configuration the configuration of the extension, from the configuration document;
    ///                      may be empty.
    /// @param context       the services this extension requires, and all contributions to
    ///                      extension points.
    /// @return a new extension.
    /// @throws RuntimeException if the extension can't be created; it's then dropped.
    Extension create(Dictionary configuration, ExtensionContext context);
}

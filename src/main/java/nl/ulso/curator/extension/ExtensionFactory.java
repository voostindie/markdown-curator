package nl.ulso.curator.extension;

import nl.ulso.dictionary.Dictionary;

/// Creates [Extension]s from configuration.
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
/// @see nl.ulso.curator.CuratorModule#CONFIGURATION_DOCUMENT_KEY
public interface ExtensionFactory
{
    /// Creates a new, fresh extension instance.
    ///
    /// @param configuration the configuration of the extension, from the configuration document;
    ///                      may be empty.
    /// @return a new extension.
    /// @throws RuntimeException if the configuration is invalid; the configuration is then
    ///                          rejected as a whole.
    Extension create(Dictionary configuration);
}

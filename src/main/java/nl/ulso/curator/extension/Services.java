package nl.ulso.curator.extension;

import java.util.List;

/// Gives an [ExtensionFactory] access to the services and contributions of the curator and of
/// other extensions, while creating an extension.
public interface Services
{
    /// @param type the type of service.
    /// @return the one and only provider of the service.
    /// @throws IllegalStateException if the service is not declared in
    ///                               [ExtensionFactory#requires()].
    <T> T get(Class<T> type);

    /// @param type the type of the extension point.
    /// @return all contributions to the extension point, from the curator and from all extensions
    /// that are loaded; possibly empty.
    <T> List<T> all(Class<T> type);
}

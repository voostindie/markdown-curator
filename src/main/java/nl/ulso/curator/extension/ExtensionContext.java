package nl.ulso.curator.extension;

import java.util.List;

/// Everything an [ExtensionFactory] gets from the curator and from other extensions while creating
/// an extension: the services it requires, and the contributions to [ExtensionPoint]s.
public interface ExtensionContext
{
    /// @param type the type of service.
    /// @return the one and only provider of the service.
    /// @throws IllegalStateException if the service is not declared in
    ///                               [ExtensionFactory#requires()].
    <T> T service(Class<T> type);

    /// @param extensionPoint the extension point.
    /// @return the values of all contributions to the extension point, from the curator and from
    /// all extensions that are loaded; possibly empty.
    <T> List<T> contributionsTo(ExtensionPoint<T> extensionPoint);
}

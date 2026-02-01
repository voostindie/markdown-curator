package nl.ulso.curator;

import java.util.ServiceLoader;

/// The [Application] discovers all curators in the system through the built-in Java [ServiceLoader]
/// mechanism. For that reason, curators must implement this interface and register the
/// implementation in the `MEYA-INF/services/nl.ulso.curator.CuratorFactory` file.
public interface CuratorFactory
{
    /// @return The name of this curator; used in logging.
    String name();

    /// Create the curator; don't implement the [Curator] yourself, instead leave it up to Dagger,
    /// like `DaggerMyOwnCurator.create().curator()`.
    Curator createCurator();
}

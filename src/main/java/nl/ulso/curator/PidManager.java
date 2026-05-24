package nl.ulso.curator;

import java.util.Collection;

/// Manages zero or more persistent identifiers (PIDs) for curators. Each curator used in the
/// application gets its own PID file. If, at startup, a PID file already exists for one of the
/// curators in the application, the application fails to start up. But two or more applications
/// that run different curators can safely run at the same time.
interface PidManager
{
    /// Checks if a PID file exists for any of the provided factories.
    boolean anyPidExists(Collection<CuratorFactory> curatorFactories);

    /// Creates a PID file for the given curator factory. The PID file will be deleted automatically
    /// when the program exits.
    ///
    /// @return true if the PID file was created successfully, false otherwise.
    boolean createPidFor(CuratorFactory curatorFactory);
}

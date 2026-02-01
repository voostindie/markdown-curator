package nl.ulso.curator.main;

import nl.ulso.curator.changelog.*;

/// Orchestrates the execution of all [ChangeProcessor]s in the system in the right order and only
/// when needed.
///
/// Starting with an initial change - typically a change to a file in the vault - all
/// [ChangeProcessor]s that consume vault changes are executed in order. In turn, they can produce
/// other changes with other, domain-specific payload types, to be picked up by other
/// [ChangeProcessor]s later in the same run. At the end of the run, the full changelog provides a
/// complete view on what has happened.
///
/// There can be many independent but related change processors in the system. At application
/// startup the orchestrator orders the processors in such a way that all requirements are
/// satisfied:
///
/// - Producers of certain payload types before consumers of these payload types.
/// - All consumed payload types have at least one producer.
///
/// When a processor is executed from a [Changelog], it receives only changes of payload types it
/// can consume. If there are none, the processor is not executed at all. When a processor produces
/// payload types other than it advocates, that is considered a programming error and will throw an
/// [IllegalStateException].
interface ChangeProcessorOrchestrator
{
    /// Runs all change processors in the system in the right order and only when needed based on
    /// the provided [Change].
    ///
    /// @return the full changelog of all changes that was built up by and applied to the change
    /// processors.
    Changelog runFor(Change<?> change);
}

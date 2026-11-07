package nl.ulso.curator.extension;

import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.statistics.MeasurementTracker;

import java.util.Set;

/// A set of [ChangeProcessor]s, [Query]s and [MeasurementTracker]s that can be loaded into and
/// unloaded from a running curator, driven by the configuration document.
///
/// The easiest way to create an extension is a Dagger [dagger.Subcomponent] that extends this
/// interface, is annotated with [ExtensionScope] and includes the [ExtensionModule]. All change
/// processors, queries and trackers in the extension must be bound into sets qualified with
/// [ExtensionBinding]; unqualified bindings end up in the parent component instead.
///
/// An extension instance has a single lifecycle: it is created when the configuration is loaded,
/// and dropped when the configuration changes. Instances are never reused across configurations.
///
/// Extensions can inject anything from the curator they are loaded into, but the curator cannot
/// inject anything from an extension. Extensions interact with each other through payload types in
/// the changelog, through [ServiceRegistration]s and through [Contribution]s.
///
/// @see ExtensionFactory
public interface Extension
{
    @ExtensionBinding
    Set<ChangeProcessor> changeProcessors();

    @ExtensionBinding
    Set<Query> queries();

    @ExtensionBinding
    Set<MeasurementTracker> measurementTrackers();

    /// @return the services this extension provides to other extensions; must match
    /// [ExtensionFactory#provides()] exactly.
    @ExtensionBinding
    Set<ServiceRegistration> services();

    /// @return the parts of this extension that need to be started and stopped.
    @ExtensionBinding
    Set<ExtensionLifecycle> lifecycles();
}

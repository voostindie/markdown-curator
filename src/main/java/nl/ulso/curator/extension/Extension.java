package nl.ulso.curator.extension;

import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.query.QueryResultFormatter;
import nl.ulso.curator.statistics.MeasurementTracker;

import java.util.Set;

/// A set of [ChangeProcessor]s, [Query]s, [QueryResultFormatter]s and [MeasurementTracker]s that
/// can be loaded into and unloaded from a running curator, driven by the configuration document.
///
/// The easiest way to create an extension is a Dagger [dagger.Subcomponent] that extends this
/// interface, is annotated with [ExtensionScope] and includes the [ExtensionModule]. All change
/// processors, queries, query result formatters and trackers in the extension must be bound into
/// sets qualified with [ExtensionBinding]; unqualified bindings end up in the parent component
/// instead.
///
/// An extension instance has a single lifecycle: it is created when the configuration is loaded,
/// and dropped when the configuration changes. Instances are never reused across configurations.
///
/// Extensions can inject anything from the curator they are loaded into, but the curator cannot
/// inject anything from an extension. Extensions interact with each other through payload types in
/// the changelog, through [ServiceRegistration]s and through [Contribution]s to [ExtensionPoint]s.
///
/// @see ExtensionFactory
public interface Extension
{
    @ExtensionBinding
    Set<ChangeProcessor> changeProcessors();

    @ExtensionBinding
    Set<Query> queries();

    /// Formatters for the query results of this extension. Typically, these format result types
    /// that are specific to the extension. Formatters of the curator take precedence over those of
    /// extensions, unless an extension formats a more specific result type.
    ///
    /// An extension can introduce a new [nl.ulso.curator.query.OutputFormat] simply by providing
    /// formatters whose output format is new. While the extension is active, the format can be
    /// used in query blocks, through the `_format` property, and the HTTP server serves it, both
    /// through content negotiation on its MIME type and through the `_format` query parameter. When
    /// the extension is unloaded, the format disappears again. Result types that the format doesn't
    /// support - like the curator's own error results - are output as Markdown instead.
    ///
    /// Format names must be unique: if an extension declares a format with the same name as an
    /// existing format, but a different MIME type or embedding, the extension is not loaded.
    /// Extensions can add formatters for an existing format, as long as they declare it in exactly
    /// the same way.
    @ExtensionBinding
    Set<QueryResultFormatter<?>> queryResultFormatters();

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

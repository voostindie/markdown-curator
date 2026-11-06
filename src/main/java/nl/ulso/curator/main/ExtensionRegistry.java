package nl.ulso.curator.main;

import dagger.Lazy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.curator.query.DynamicQueryCatalog;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.statistics.MeasurementTracker;
import nl.ulso.curator.statistics.MeasurementTrackerRegistry;

import java.util.*;

import static java.util.Collections.unmodifiableSortedSet;

/// Keeps track of the [Extension]s that are loaded into the curator.
///
/// Loading a new set of extensions is a two-step process:
///
/// 1. The [ExtensionLoader] reads the configuration, creates the extensions and stages them here.
/// 2. The [DefaultChangeProcessorOrchestrator] picks up the staged extensions at a safe moment -
/// while processing a [nl.ulso.curator.change.Reset] - verifies them, orders their change
/// processors and then activates them. That registers their queries in the query catalog and
/// their trackers in the statistics.
///
/// If anything goes wrong in either step, the new configuration is rejected as a whole and the
/// current set of extensions stays active.
///
/// Staging and activation happen on the curator thread. The [#status()] can be read from any
/// thread.
@Singleton
final class ExtensionRegistry
{
    private final Lazy<DynamicQueryCatalog> queryCatalog;
    private final MeasurementTrackerRegistry trackerRegistry;
    private final SortedSet<String> availableExtensions;
    private Map<String, Extension> staged;
    private volatile Status status;

    /// The query catalog is injected lazily, to break the dependency cycle: the catalog contains
    /// all queries, some of which depend - indirectly - on this registry.
    @Inject
    ExtensionRegistry(
        Lazy<DynamicQueryCatalog> queryCatalog,
        MeasurementTrackerRegistry trackerRegistry,
        Map<String, ExtensionFactory> extensionFactories)
    {
        this.queryCatalog = queryCatalog;
        this.trackerRegistry = trackerRegistry;
        this.availableExtensions = unmodifiableSortedSet(new TreeSet<>(extensionFactories.keySet()));
        this.staged = null;
        this.status = new Status(availableExtensions, Collections.emptySortedSet(), null);
    }

    /// Stages a new set of extensions, replacing any previously staged set.
    void stage(Map<String, Extension> extensions)
    {
        staged = Map.copyOf(extensions);
    }

    /// @return the staged extensions, if any; the staged set is cleared.
    Optional<Map<String, Extension>> takeStaged()
    {
        var result = Optional.ofNullable(staged);
        staged = null;
        return result;
    }

    /// Activates a set of extensions, replacing the currently active set.
    ///
    /// @throws IllegalArgumentException if the queries of the extensions clash with existing
    ///                                  queries. Nothing is changed in that case.
    void activate(Map<String, Extension> extensions)
    {
        var queries = new ArrayList<Query>();
        var trackers = new ArrayList<MeasurementTracker>();
        extensions.values().forEach(extension -> {
            queries.addAll(extension.queries());
            trackers.addAll(extension.measurementTrackers());
        });
        queryCatalog.get().replaceDynamicQueries(queries);
        trackerRegistry.replaceDynamicTrackers(trackers);
        status = new Status(
            availableExtensions,
            unmodifiableSortedSet(new TreeSet<>(extensions.keySet())),
            null
        );
    }

    /// Records that a new configuration was rejected; the active set of extensions stays as is.
    void reject(String reason)
    {
        staged = null;
        status = new Status(status.available(), status.active(), reason);
    }

    Status status()
    {
        return status;
    }

    /// Snapshot of the state of the registry, for reporting.
    ///
    /// @param available the names of all extensions that can be enabled.
    /// @param active    the names of all extensions that are currently enabled.
    /// @param error     the reason the last configuration was rejected, or `null` if the last
    ///                  configuration was accepted.
    record Status(SortedSet<String> available, SortedSet<String> active, String error)
    {
        Optional<String> lastError()
        {
            return Optional.ofNullable(error);
        }
    }
}

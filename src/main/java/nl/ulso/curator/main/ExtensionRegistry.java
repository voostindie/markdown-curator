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

import static java.util.Collections.*;

/// Keeps track of the [Generation] of extensions that is active in the curator.
///
/// Loading a new generation is a two-step process:
///
/// 1. The [ConfigurationLoader] reads the configuration, resolves it into a new generation through
/// the [ExtensionResolver] and stages it here.
/// 2. The [DefaultChangeProcessorOrchestrator] picks up the staged generation at a safe moment -
/// while processing a [nl.ulso.curator.change.Reset] - drops extensions whose change processors
/// or queries don't fit, and then activates the generation. That registers its queries in the
/// query catalog and its trackers in the statistics, stops the previous generation and starts the
/// new one.
///
/// Staging and activation happen on the curator thread. The [#status()] can be read from any
/// thread.
@Singleton
final class ExtensionRegistry
{
    private final Lazy<DynamicQueryCatalog> queryCatalog;
    private final MeasurementTrackerRegistry trackerRegistry;
    private final SortedMap<String, ExtensionFactory> availableExtensions;
    private Generation staged;
    private Generation active;
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
        this.availableExtensions = unmodifiableSortedMap(new TreeMap<>(extensionFactories));
        this.staged = null;
        this.active = Generation.EMPTY;
        this.status = new Status(availableExtensions, emptySortedSet(), emptySortedMap(), null);
    }

    /// Stages a new generation, replacing any previously staged one.
    void stage(Generation generation)
    {
        staged = generation;
    }

    /// @return the staged generation, if any; the staged generation is cleared.
    Optional<Generation> takeStaged()
    {
        var result = Optional.ofNullable(staged);
        staged = null;
        return result;
    }

    /// Drops extensions from the generation whose queries have names that are already taken, by
    /// the curator or by extensions earlier in the generation.
    void dropConflictingQueries(Generation generation)
    {
        var taken = new HashSet<>(queryCatalog.get().staticQueryNames());
        for (var entry : List.copyOf(generation.extensions().entrySet()))
        {
            var name = entry.getKey();
            if (!generation.extensions().containsKey(name))
            {
                continue;   // Dropped as a dependent of an earlier extension.
            }
            var queryNames = entry.getValue().queries().stream().map(Query::name).toList();
            var conflicts = queryNames.stream()
                .filter(queryName -> taken.contains(queryName) ||
                                     queryNames.indexOf(queryName) !=
                                     queryNames.lastIndexOf(queryName))
                .distinct()
                .sorted()
                .toList();
            if (conflicts.isEmpty())
            {
                taken.addAll(queryNames);
            }
            else
            {
                generation.drop(name, "Query name(s) already in use: " +
                                      String.join(", ", conflicts) + ".");
            }
        }
    }

    /// Activates a generation, replacing the currently active one, which is stopped. The new
    /// generation is started.
    void activate(Generation generation)
    {
        var queries = new ArrayList<Query>();
        var trackers = new ArrayList<MeasurementTracker>();
        generation.extensions().values().forEach(extension -> {
            queries.addAll(extension.queries());
            trackers.addAll(extension.measurementTrackers());
        });
        queryCatalog.get().replaceDynamicQueries(queries);
        trackerRegistry.replaceDynamicTrackers(trackers);
        active.stop();
        active = generation;
        active.start();
        status = new Status(
            availableExtensions,
            unmodifiableSortedSet(new TreeSet<>(generation.extensions().keySet())),
            generation.failures(),
            null
        );
    }

    /// Records that a new configuration was rejected as a whole; the active generation stays.
    void reject(String reason)
    {
        staged = null;
        status = new Status(status.available(), status.active(), status.failures(), reason);
    }

    /// @return the extensions in the active generation, in creation order.
    Collection<Extension> activeExtensions()
    {
        return active.extensions().values();
    }

    Status status()
    {
        return status;
    }

    /// Snapshot of the state of the registry, for reporting.
    ///
    /// @param available the factories of all extensions that can be enabled, by name.
    /// @param active    the names of all extensions that are currently loaded.
    /// @param failures  for each enabled extension that couldn't be loaded, the reason.
    /// @param error     the reason the last configuration was rejected as a whole, or `null` if
    ///                  it was accepted.
    record Status(
        SortedMap<String, ExtensionFactory> available,
        SortedSet<String> active,
        SortedMap<String, String> failures,
        String error)
    {
        Optional<String> lastError()
        {
            return Optional.ofNullable(error);
        }
    }
}

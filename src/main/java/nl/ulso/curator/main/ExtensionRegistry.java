package nl.ulso.curator.main;

import dagger.Lazy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.curator.query.DynamicQueryCatalog;
import nl.ulso.curator.query.DynamicQueryResultFormatterRegistry;
import nl.ulso.curator.query.OutputFormat;
import nl.ulso.curator.query.Query;
import nl.ulso.curator.query.QueryResultFormatter;
import nl.ulso.curator.statistics.MeasurementTracker;
import nl.ulso.curator.statistics.MeasurementTrackerRegistry;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toSet;

/// Keeps track of the [Generation] of extensions that is active in the curator.
///
/// Loading a new generation is a two-step process:
///
/// 1. The [ConfigurationLoader] reads the configuration, resolves it into a new generation through
/// the [ExtensionResolver] and stages it here.
/// 2. The [DefaultChangeProcessorOrchestrator] picks up the staged generation at a safe moment -
/// while processing a [nl.ulso.curator.change.Reset] - drops extensions whose change processors,
/// queries or output formats don't fit, and then activates the generation. That registers its
/// queries in the query catalog, its query result formatters in the formatter registry and its
/// trackers in the statistics, stops the previous generation and starts the new one.
///
/// Staging and activation happen on the curator thread. The [#status()] can be read from any
/// thread.
@Singleton
final class ExtensionRegistry
{
    private final Lazy<DynamicQueryCatalog> queryCatalog;
    private final DynamicQueryResultFormatterRegistry formatterRegistry;
    private final MeasurementTrackerRegistry trackerRegistry;
    private final SortedMap<String, ExtensionFactory> availableExtensions;
    private Generation staged;
    private Generation active;
    private final AtomicReference<Status> status;

    /// The query catalog is injected lazily, to break the dependency cycle: the catalog contains
    /// all queries, some of which depend - indirectly - on this registry.
    @Inject
    ExtensionRegistry(
        Lazy<DynamicQueryCatalog> queryCatalog,
        DynamicQueryResultFormatterRegistry formatterRegistry,
        MeasurementTrackerRegistry trackerRegistry,
        Map<String, ExtensionFactory> extensionFactories)
    {
        this.queryCatalog = queryCatalog;
        this.formatterRegistry = formatterRegistry;
        this.trackerRegistry = trackerRegistry;
        this.availableExtensions = unmodifiableSortedMap(new TreeMap<>(extensionFactories));
        this.staged = null;
        this.active = Generation.EMPTY;
        this.status = new AtomicReference<>(new Status(
            availableExtensions, emptySortedSet(), emptySortedMap(), emptySortedMap(), null));
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

    /// Finds the extensions in the generation that conflict with the curator or with other
    /// extensions in the generation, without dropping them. Extensions conflict if:
    ///
    /// - One of their queries has a name that is already taken, or that they use twice.
    /// - One of their query result formatters declares an output format whose name is already
    ///   taken by a different format, for example one with a different MIME type. Extensions can
    ///   freely add formatters for an existing format, as long as they declare it in exactly the
    ///   same way.
    ///
    /// Extensions are checked in creation order; the first one to claim a name wins. All checks
    /// are made against the extensions that were accepted before, so an extension that loses one
    /// conflict never causes another one to lose: it doesn't claim any names at all. The extensions
    /// that depend on the services of an extension that loses are left out as well, without a
    /// reason of their own; dropping the loser from the generation drops them too. Providers are
    /// always created before their consumers, so a single pass suffices.
    ///
    /// Nothing is dropped here, because the outcome depends on which extensions are in the
    /// generation: if an extension that wins a conflict is dropped later for a different reason,
    /// the extension that lost might fit after all.
    ///
    /// @return for each extension that loses a conflict, the reason, in creation order.
    SequencedMap<String, String> findConflicts(Generation generation)
    {
        var takenQueries = new HashSet<>(queryCatalog.get().staticQueryNames());
        var takenFormats = new HashMap<String, OutputFormat>();
        formatterRegistry.staticOutputFormats()
            .forEach(format -> takenFormats.put(format.name(), format));
        var excluded = new HashSet<String>();
        var conflicts = new LinkedHashMap<String, String>();
        generation.extensions().forEach((name, extension) -> {
            if (excluded.contains(name))
            {
                return;     // Depends on an extension that lost a conflict.
            }
            var queryNames = extension.queries().stream().map(Query::name).toList();
            var queryConflicts = findQueryConflicts(queryNames, takenQueries);
            var declaredFormats = new HashMap<String, OutputFormat>();
            var formats = extension.queryResultFormatters().stream()
                .map(QueryResultFormatter::outputFormat)
                .toList();
            var formatConflicts = findFormatConflicts(formats, takenFormats, declaredFormats);
            if (queryConflicts.isEmpty() && formatConflicts.isEmpty())
            {
                takenQueries.addAll(queryNames);
                takenFormats.putAll(declaredFormats);
                return;
            }
            conflicts.put(name, conflictReason(queryConflicts, formatConflicts));
            excluded.addAll(generation.withDependents(name));
        });
        return unmodifiableSequencedMap(conflicts);
    }

    private static List<String> findQueryConflicts(
        List<String> queryNames, Set<String> takenQueries)
    {
        return queryNames.stream()
            .filter(queryName -> takenQueries.contains(queryName) ||
                                 queryNames.indexOf(queryName) !=
                                 queryNames.lastIndexOf(queryName))
            .distinct()
            .sorted()
            .toList();
    }

    private static SortedSet<String> findFormatConflicts(
        List<OutputFormat> formats, Map<String, OutputFormat> takenFormats,
        Map<String, OutputFormat> declaredFormats)
    {
        var formatConflicts = new TreeSet<String>();
        for (var format : formats)
        {
            var existing =
                takenFormats.getOrDefault(format.name(), declaredFormats.get(format.name()));
            if (existing != null && !existing.equals(format))
            {
                formatConflicts.add(format.name());
            }
            declaredFormats.putIfAbsent(format.name(), format);
        }
        return formatConflicts;
    }

    private static String conflictReason(List<String> queryConflicts, Set<String> formatConflicts)
    {
        var reasons = new ArrayList<String>();
        if (!queryConflicts.isEmpty())
        {
            reasons.add("Query name(s) already in use: " +
                        String.join(", ", queryConflicts) + ".");
        }
        if (!formatConflicts.isEmpty())
        {
            reasons.add("Output format(s) already in use with a different definition: " +
                        String.join(", ", formatConflicts) + ".");
        }
        return String.join(" ", reasons);
    }

    /// Activates a generation, replacing the currently active one, which is stopped. The new
    /// generation is started.
    void activate(Generation generation)
    {
        var queries = new ArrayList<Query>();
        var formatters = new ArrayList<QueryResultFormatter<?>>();
        var trackers = new ArrayList<MeasurementTracker>();
        generation.extensions().values().forEach(extension -> {
            queries.addAll(extension.queries());
            formatters.addAll(extension.queryResultFormatters());
            trackers.addAll(extension.measurementTrackers());
        });
        // Formatters first, so that HTTP requests never run a new query before its formatters are
        // registered.
        formatterRegistry.replaceDynamicFormatters(formatters);
        queryCatalog.get().replaceDynamicQueries(queries);
        trackerRegistry.replaceDynamicTrackers(trackers);
        active.stop();
        active = generation;
        active.start();
        status.set(new Status(
            availableExtensions,
            unmodifiableSortedSet(new TreeSet<>(generation.extensions().keySet())),
            generation.failures(),
            outputFormatProviders(generation),
            null
        ));
    }

    /// @return for each output format that is introduced by extensions - so not a static one - the
    /// names of the extensions that provide formatters for it.
    private SortedMap<String, SortedSet<String>> outputFormatProviders(Generation generation)
    {
        var staticFormats = formatterRegistry.staticOutputFormats().stream()
            .map(OutputFormat::name)
            .collect(toSet());
        var providers = new TreeMap<String, SortedSet<String>>();
        generation.extensions().forEach((name, extension) ->
            extension.queryResultFormatters().stream()
                .map(formatter -> formatter.outputFormat().name())
                .filter(format -> !staticFormats.contains(format))
                .forEach(format -> providers.computeIfAbsent(format, _ -> new TreeSet<>())
                    .add(name)));
        var result = new TreeMap<String, SortedSet<String>>();
        providers.forEach((format, names) -> result.put(format, unmodifiableSortedSet(names)));
        return unmodifiableSortedMap(result);
    }

    /// Records that a new configuration was rejected as a whole; the active generation stays.
    void reject(String reason)
    {
        staged = null;
        var current = status.get();
        status.set(new Status(current.available(), current.active(), current.failures(),
            current.outputFormatProviders(), reason));
    }

    /// @return the extensions in the active generation, in creation order.
    Collection<Extension> activeExtensions()
    {
        return active.extensions().values();
    }

    Status status()
    {
        return status.get();
    }

    /// Snapshot of the state of the registry, for reporting.
    ///
    /// @param available the factories of all extensions that can be enabled, by name.
    /// @param active    the names of all extensions that are currently loaded.
    /// @param failures  for each enabled extension that couldn't be loaded, the reason.
    /// @param outputFormatProviders for each output format introduced by extensions, the names of
    ///                  the extensions that provide formatters for it.
    /// @param error     the reason the last configuration was rejected as a whole, or `null` if
    ///                  it was accepted.
    record Status(
        SortedMap<String, ExtensionFactory> available,
        SortedSet<String> active,
        SortedMap<String, String> failures,
        SortedMap<String, SortedSet<String>> outputFormatProviders,
        String error)
    {
        Optional<String> lastError()
        {
            return Optional.ofNullable(error);
        }
    }
}

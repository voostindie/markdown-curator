package nl.ulso.curator.query;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static org.slf4j.LoggerFactory.getLogger;

/// Default [DynamicQueryResultFormatterRegistry].
///
/// Static formatters are injected by Dagger; dynamic formatters are registered at runtime, by
/// extensions. Because results are formatted concurrently - by the query orchestrator and by the
/// HTTP server - the formatters are never modified in place: they're replaced by a new, immutable
/// snapshot. For every output format, static formatters come before dynamic ones.
///
/// Formats are keyed by name. The first formatter for a name determines the format; formatters for
/// a different format with the same name are ignored.
@Singleton
final class DefaultQueryResultFormatterRegistry
    implements DynamicQueryResultFormatterRegistry
{
    private static final Logger LOGGER = getLogger(DefaultQueryResultFormatterRegistry.class);

    private final List<QueryResultFormatter<?>> staticFormatters;
    private final Set<OutputFormat> staticOutputFormats;
    private volatile Snapshot snapshot;

    @Inject
    DefaultQueryResultFormatterRegistry(Set<QueryResultFormatter<?>> formatters)
    {
        this.staticFormatters = List.copyOf(formatters);
        this.snapshot = Snapshot.of(staticFormatters.stream());
        this.staticOutputFormats = snapshot.outputFormats();
    }

    @Override
    public void replaceDynamicFormatters(Collection<QueryResultFormatter<?>> dynamicFormatters)
    {
        snapshot = Snapshot.of(
            Stream.concat(staticFormatters.stream(), List.copyOf(dynamicFormatters).stream()));
    }

    @Override
    public Set<OutputFormat> staticOutputFormats()
    {
        return staticOutputFormats;
    }

    @Override
    public Set<OutputFormat> outputFormats()
    {
        return snapshot.outputFormats();
    }

    @Override
    public Optional<OutputFormat> outputFormat(String name)
    {
        if (name == null)
        {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshot.groups().get(name.strip().toLowerCase(Locale.ROOT)))
            .map(FormatGroup::outputFormat);
    }

    @Override
    public boolean supports(QueryResult queryResult, OutputFormat outputFormat)
    {
        return snapshot.findFormatter(queryResult, outputFormat.name()).isPresent();
    }

    @Override
    public FormattedResult formatResult(QueryResult queryResult, OutputFormat outputFormat)
    {
        var current = snapshot;
        return current.findFormatter(queryResult, outputFormat.name())
            .or(() -> fallBackToMarkdown(current, queryResult, outputFormat))
            .map(selection -> selection.apply(queryResult))
            .orElseGet(() -> noOutputFormatterAvailable(queryResult, outputFormat));
    }

    private static Optional<Selection> fallBackToMarkdown(
        Snapshot snapshot, QueryResult queryResult, OutputFormat outputFormat)
    {
        if (outputFormat.name().equals(MARKDOWN.name()))
        {
            return Optional.empty();
        }
        var fallback = snapshot.findFormatter(queryResult, MARKDOWN.name());
        if (fallback.isPresent() && LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Output format '{}' doesn't support query result type '{}'; " +
                         "falling back to Markdown.",
                outputFormat.name(), queryResult.getClass().getName()
            );
        }
        return fallback;
    }

    private static FormattedResult noOutputFormatterAvailable(
        QueryResult queryResult, OutputFormat outputFormat)
    {
        if (LOGGER.isErrorEnabled())
        {
            LOGGER.error(
                "No output formatter for '{}' available for query result type '{}'.",
                outputFormat.name(),
                queryResult.getClass().getName()
            );
        }
        return new FormattedResult(MARKDOWN, new ErrorMarkdownFormatter().format(new ErrorResult(
            "No output formatter for '" + outputFormat.name() +
            "' available for query result type '" + queryResult.getClass().getSimpleName() +
            "'.")));
    }

    /// All formatters for a single output format, in order of registration.
    private record FormatGroup(OutputFormat outputFormat, List<QueryResultFormatter<?>> formatters)
    {
    }

    /// A formatter selected for a query result, and the format it produces.
    private record Selection(OutputFormat outputFormat, QueryResultFormatter<?> formatter)
    {
        FormattedResult apply(QueryResult queryResult)
        {
            QueryResultFormatter<QueryResult> typed = safeUncheckedCast(formatter);
            return new FormattedResult(outputFormat,
                typed.format(typed.queryResultType().cast(queryResult)));
        }

        @SuppressWarnings("unchecked")
        private static <T extends QueryResult> QueryResultFormatter<T> safeUncheckedCast(
            QueryResultFormatter<?> formatter)
        {
            return (QueryResultFormatter<T>) formatter;
        }
    }

    /// Immutable snapshot of all registered formatters, grouped by output format name.
    private record Snapshot(Map<String, FormatGroup> groups, Set<OutputFormat> outputFormats)
    {
        static Snapshot of(Stream<QueryResultFormatter<?>> formatters)
        {
            var formats = new LinkedHashMap<String, OutputFormat>();
            var lists = new LinkedHashMap<String, List<QueryResultFormatter<?>>>();
            formatters.forEach(formatter -> {
                var format = formatter.outputFormat();
                var existing = formats.putIfAbsent(format.name(), format);
                if (existing != null && !existing.equals(format))
                {
                    LOGGER.warn("Ignoring formatter '{}': its output format {} conflicts with " +
                                "the already registered format {}.",
                        formatter.getClass().getName(), format, existing
                    );
                    return;
                }
                lists.computeIfAbsent(format.name(), _ -> new ArrayList<>()).add(formatter);
            });
            var groups = new LinkedHashMap<String, FormatGroup>();
            lists.forEach((name, list) ->
                groups.put(name, new FormatGroup(formats.get(name), List.copyOf(list))));
            return new Snapshot(
                unmodifiableMap(groups),
                unmodifiableSet(new LinkedHashSet<>(formats.values()))
            );
        }

        /// @return the formatter with the most specific query result type for the given result in
        /// the given format; the first one on a tie.
        Optional<Selection> findFormatter(QueryResult queryResult, String formatName)
        {
            var group = groups.get(formatName);
            if (group == null)
            {
                return Optional.empty();
            }
            return group.formatters().stream()
                .filter(formatter -> formatter.queryResultType().isInstance(queryResult))
                .reduce(Snapshot::mostSpecific)
                .map(formatter -> new Selection(group.outputFormat(), formatter));
        }

        private static QueryResultFormatter<?> mostSpecific(
            QueryResultFormatter<?> best, QueryResultFormatter<?> candidate)
        {
            var bestType = best.queryResultType();
            var candidateType = candidate.queryResultType();
            return bestType != candidateType && bestType.isAssignableFrom(candidateType)
                   ? candidate
                   : best;
        }
    }
}

package nl.ulso.curator.query;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.slf4j.LoggerFactory.getLogger;

/// Default [DynamicQueryResultFormatterRegistry].
///
/// Static formatters are injected by Dagger; dynamic formatters are registered at runtime, by
/// extensions. Because results are formatted concurrently - by the query orchestrator and by the
/// HTTP server - the formatters are never modified in place: they're replaced by a new, immutable
/// map. For every output format, static formatters come before dynamic ones.
@Singleton
final class DefaultQueryResultFormatterRegistry
    implements DynamicQueryResultFormatterRegistry
{
    private static final Logger LOGGER = getLogger(DefaultQueryResultFormatterRegistry.class);

    private final List<QueryResultFormatter<?>> staticFormatters;
    private volatile Map<OutputFormat, List<QueryResultFormatter<?>>> formatters;

    @Inject
    DefaultQueryResultFormatterRegistry(Set<QueryResultFormatter<?>> formatters)
    {
        this.staticFormatters = List.copyOf(formatters);
        this.formatters = groupByOutputFormat(staticFormatters.stream());
    }

    @Override
    public void replaceDynamicFormatters(Collection<QueryResultFormatter<?>> dynamicFormatters)
    {
        formatters = groupByOutputFormat(
            Stream.concat(staticFormatters.stream(), List.copyOf(dynamicFormatters).stream()));
    }

    private static Map<OutputFormat, List<QueryResultFormatter<?>>> groupByOutputFormat(
        Stream<QueryResultFormatter<?>> formatters)
    {
        return unmodifiableMap(formatters.collect(groupingBy(
            QueryResultFormatter::outputFormat, LinkedHashMap::new, toUnmodifiableList())));
    }

    @Override
    public Set<OutputFormat> outputFormats()
    {
        return formatters.keySet();
    }

    @Override
    public boolean supports(QueryResult queryResult, OutputFormat outputFormat)
    {
        return resolveFormattersFor(outputFormat).stream()
            .anyMatch(supportsQueryResult(queryResult));
    }

    @Override
    public String format(QueryResult queryResult, OutputFormat outputFormat)
    {
        return resolveFormattersFor(outputFormat).stream()
            .filter(supportsQueryResult(queryResult))
            .reduce(DefaultQueryResultFormatterRegistry::mostSpecific)
            .map(this::safeUncheckedCast)
            .map(formatter -> formatter.format(formatter.queryResultType().cast(queryResult)))
            .orElseGet(() -> noOutputFormatterAvailable(queryResult, outputFormat));
    }

    /// @return the formatter with the most specific query result type; the first one on a tie.
    private static QueryResultFormatter<?> mostSpecific(
        QueryResultFormatter<?> best, QueryResultFormatter<?> candidate)
    {
        var bestType = best.queryResultType();
        var candidateType = candidate.queryResultType();
        return bestType != candidateType && bestType.isAssignableFrom(candidateType)
               ? candidate
               : best;
    }

    private List<QueryResultFormatter<?>> resolveFormattersFor(OutputFormat outputFormat)
    {
        var snapshot = formatters;
        var resultFormatters = snapshot.get(outputFormat);
        if (resultFormatters == null)
        {
            // Try again, but now on mimetype (a little more expensive)
            resultFormatters = snapshot.entrySet().stream()
                .filter(entry -> entry.getKey().mimeType().equals(outputFormat.mimeType()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
        }
        return resultFormatters == null ? emptyList() : resultFormatters;
    }

    private static Predicate<QueryResultFormatter<?>> supportsQueryResult(QueryResult queryResult)
    {
        return formatter -> formatter.queryResultType().isInstance(queryResult);
    }

    @SuppressWarnings("unchecked")
    private <T extends QueryResult> QueryResultFormatter<T> safeUncheckedCast(
        QueryResultFormatter<?> formatter)
    {
        return (QueryResultFormatter<T>) formatter;
    }

    private String noOutputFormatterAvailable(QueryResult queryResult, OutputFormat outputFormat)
    {
        if (LOGGER.isErrorEnabled())
        {
            LOGGER.error(
                "No output formatter for '{}' available for query result type '{}'.",
                outputFormat.name(),
                queryResult.getClass().getName()
            );
        }
        return new ErrorMarkdownFormatter().format(new ErrorResult(
            "No output formatter for '" + outputFormat.name() +
            "' available for query result type '" + queryResult.getClass().getSimpleName() + "'."));
    }
}

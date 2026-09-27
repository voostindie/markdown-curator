package nl.ulso.curator.query;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
final class DefaultQueryResultFormatterRegistry
    implements QueryResultFormatterRegistry
{
    private static final Logger LOGGER = getLogger(DefaultQueryResultFormatterRegistry.class);

    private final Map<OutputFormat, List<QueryResultFormatter<?>>> formatters;

    @Inject
    DefaultQueryResultFormatterRegistry(Set<QueryResultFormatter<?>> formatters)
    {
        this.formatters = formatters.stream()
            .collect(groupingBy(QueryResultFormatter::outputFormat));
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
            .findFirst()
            .map(this::safeUncheckedCast)
            .map(formatter -> formatter.format(formatter.queryResultType().cast(queryResult)))
            .orElseGet(() -> noOutputFormatterAvailable(queryResult, outputFormat));
    }

    private List<QueryResultFormatter<?>> resolveFormattersFor(OutputFormat outputFormat)
    {
        var resultFormatters = formatters.get(outputFormat);
        if (resultFormatters == null)
        {
            // Try again, but now on mimetype (a little more expensive)
            resultFormatters = formatters.entrySet().stream()
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
    private <QR extends QueryResult> QueryResultFormatter<QR> safeUncheckedCast(
        QueryResultFormatter<?> formatter)
    {
        return (QueryResultFormatter<QR>) formatter;
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

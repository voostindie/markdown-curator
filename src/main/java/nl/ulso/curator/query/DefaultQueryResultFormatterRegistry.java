package nl.ulso.curator.query;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.util.*;

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
    public String format(QueryResult queryResult, OutputFormat outputFormat)
    {
        var outputFormatters = formatters.get(outputFormat);
        if (outputFormatters == null)
        {
            return noOutputFormattersAvailable(outputFormat);
        }
        return outputFormatters.stream()
            .filter(formatter -> formatter.queryResultType().isInstance(queryResult))
            .findFirst()
            .map(this::safeUncheckedCast)
            .map(formatter -> formatter.format(formatter.queryResultType().cast(queryResult)))
            .orElseGet(() -> noOutputFormatterAvailableForResultType(outputFormat, queryResult));
    }

    private String noOutputFormattersAvailable(OutputFormat outputFormat)
    {
        if (LOGGER.isErrorEnabled())
        {
            LOGGER.error("Unknown output format '{}'.", outputFormat.name());
        }
        return new ErrorMarkdownFormatter().format(new ErrorResult(
            "Unknown output format '" + outputFormat.name() + "'."));
    }

    @SuppressWarnings("unchecked")
    private <QR extends QueryResult> QueryResultFormatter<QR> safeUncheckedCast(
        QueryResultFormatter<?> formatter)
    {
        return (QueryResultFormatter<QR>) formatter;
    }

    private String noOutputFormatterAvailableForResultType(
        OutputFormat outputFormat, QueryResult queryResult)
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

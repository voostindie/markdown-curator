package nl.ulso.markdown_curator.query;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;

public class QueryResultFactory
{
    private static final String COMMENT_START = "<!-- ";
    private static final String COMMENT_END = " -->";
    private static final String PERFORMANCE_WARNING =
            "WARNING: do not overuse this query; it is slow!";
    private final GeneralMessages messages;

    public QueryResultFactory()
    {
        this(new ResourceBundledGeneralMessages());
    }

    @Inject
    public QueryResultFactory(GeneralMessages messages)
    {
        this.messages = messages;
    }

    /**
     * @return an empty result; it shows that there are no results.
     */
    public QueryResult empty()
    {
        return new EmptyResult(messages.noResults());
    }

    /**
     * Creates an error result; it adds a level-3 "Error" header at the top.
     *
     * @param errorMessage The message to show in the error.
     * @return QueryResult that represents an error.
     */
    public QueryResult error(String errorMessage)
    {
        return new ErrorResult(errorMessage);
    }

    /**
     * Creates a Markdown table, nicely formatted.
     *
     * @param columns Columns to show in the table, in this order.
     * @param rows    Data for the table: a map for each row, with the column as the key and the
     *                content as the value.
     * @return QueryResult that outputs a table.
     */
    public QueryResult table(List<String> columns, List<Map<String, String>> rows)
    {
        if (rows.isEmpty())
        {
            return empty();
        }
        return new TableResult(columns, rows, messages.resultSummary(rows.size()));
    }

    /**
     * Creates an unordered Markdown list
     *
     * @param rows Data for the list; each value is written as is.
     * @return QueryResult that outputs a list.
     */
    public QueryResult unorderedList(List<String> rows)
    {
        if (rows.isEmpty())
        {
            return empty();
        }
        return new UnorderedListResult(rows);
    }

    public QueryResult string(String output)
    {
        if (output.isBlank())
        {
            return empty();
        }
        return () -> output;
    }

    public QueryResult stringWithSummary(String output, int resultCount)
    {
        if (resultCount == 0)
        {
            return empty();
        }
        return () -> output + lineSeparator() + "(*" + messages.resultSummary(resultCount) + "*)";
    }

    public QueryResult withPerformanceWarning(QueryResult slowQueryResult)
    {
        return wrapSlowQueryResult(slowQueryResult);
    }

    public QueryResultFactory withPerformanceWarning()
    {
        return new QueryResultFactory()
        {
            @Override
            public QueryResult empty()
            {
                return super.empty();
            }

            @Override
            public QueryResult error(String errorMessage)
            {
                return super.error(errorMessage);
            }

            @Override
            public QueryResult table(List<String> columns, List<Map<String, String>> rows)
            {
                return wrapSlowQueryResult(super.table(columns, rows));
            }

            @Override
            public QueryResult unorderedList(List<String> rows)
            {
                return wrapSlowQueryResult(super.unorderedList(rows));
            }

            @Override
            public QueryResult string(String output)
            {
                return wrapSlowQueryResult(super.string(output));
            }

            @Override
            public QueryResult stringWithSummary(String output, int resultCount)
            {
                return wrapSlowQueryResult(super.stringWithSummary(output, resultCount));
            }

            @Override
            public QueryResultFactory withPerformanceWarning()
            {
                return this;
            }

            @Override
            public QueryResult withPerformanceWarning(QueryResult slowQueryResult)
            {
                return slowQueryResult;
            }
        };
    }

    private QueryResult wrapSlowQueryResult(QueryResult slowQueryResult)
    {
        return () -> slowQueryResult.toMarkdown().trim() + lineSeparator() + lineSeparator() +
                     COMMENT_START + PERFORMANCE_WARNING + COMMENT_END;
    }

}

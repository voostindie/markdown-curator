package nl.ulso.curator.query;

import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;

class QueryResultFactoryImpl
    implements QueryResultFactory
{
    private static final String COMMENT_START = "<!-- ";
    private static final String COMMENT_END = " -->";
    private static final String PERFORMANCE_WARNING =
            "WARNING: do not overuse this query; it is slow!";
    private final GeneralMessages messages;

    public QueryResultFactoryImpl()
    {
        this(new ResourceBundledGeneralMessages());
    }

    @Inject
    public QueryResultFactoryImpl(GeneralMessages messages)
    {
        this.messages = messages;
    }

    @Override
    public QueryResult empty()
    {
        return new EmptyResult(messages.noResults());
    }

    @Override
    public QueryResult error(String errorMessage)
    {
        return new ErrorResult(errorMessage);
    }

    @Override
    public QueryResult table(List<String> columns, List<Map<String, String>> rows)
    {
        if (rows.isEmpty())
        {
            return empty();
        }
        return new TableResult(columns, rows);
    }

    @Override
    public QueryResult table(
        List<String> columns, List<Alignment> alignments,
        List<Map<String, String>> rows)
    {
        if (rows.isEmpty())
        {
            return empty();
        }
        return new TableResult(columns, alignments, rows);
    }

    @Override
    public QueryResult unorderedList(List<String> rows)
    {
        if (rows.isEmpty())
        {
            return empty();
        }
        return new UnorderedListResult(rows);
    }

    @Override
    public QueryResult string(String output)
    {
        if (output.isBlank())
        {
            return empty();
        }
        return () -> output;
    }

    @Override
    public QueryResult withPerformanceWarning(QueryResult slowQueryResult)
    {
        return wrapSlowQueryResult(slowQueryResult);
    }

    @Override
    public QueryResultFactory withPerformanceWarning()
    {
        return new QueryResultFactoryImpl()
        {
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
            public QueryResultFactoryImpl withPerformanceWarning()
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

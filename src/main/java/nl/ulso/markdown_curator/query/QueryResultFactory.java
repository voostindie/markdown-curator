package nl.ulso.markdown_curator.query;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public final class QueryResultFactory
{
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
     * Creates a no-op result; it throws an exception when asked for its Markdown content. When a
     * query returns this output, the curator will do nothing and keep the existing content in
     * place.
     *
     * @return QueryResult that represents a no-op.
     */
    public QueryResult noOp()
    {
        return new NoOpResult();
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
}

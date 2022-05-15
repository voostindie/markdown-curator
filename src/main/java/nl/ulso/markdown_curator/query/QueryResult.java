package nl.ulso.markdown_curator.query;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of running a query.
 */
@FunctionalInterface
public interface QueryResult
{
    /**
     * Creates an error result; it add a level-3 "Error" header at the top.
     *
     * @param errorMessage The message to show in the error.
     * @return QueryResult that represents an error.
     */
    static QueryResult error(String errorMessage)
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
    static QueryResult table(List<String> columns, List<Map<String, String>> rows)
    {
        return new TableResult(columns, rows);
    }

    /**
     * Creates an unordered Markdown list
     *
     * @param rows Data for the list; each value is written as is.
     * @return QueryResult that outputs a list.
     */
    static QueryResult unorderedList(List<String> rows)
    {
        return new UnorderedListResult(rows);
    }

    /**
     * @return A Markdown representation of the result.
     */
    String toMarkdown();
}

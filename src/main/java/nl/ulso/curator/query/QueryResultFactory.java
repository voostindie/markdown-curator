package nl.ulso.curator.query;

import java.util.List;
import java.util.Map;

public interface QueryResultFactory
{
    /// @return an empty result; it shows that there are no results.
    QueryResult empty();

    /// Creates an error result; it adds a level-3 "Error" header at the top.
    ///
    /// @param errorMessage The message to show in the error.
    /// @return QueryResult that represents an error.
    QueryResult error(String errorMessage);

    /// Creates a Markdown table, nicely formatted.
    ///
    /// @param columns Columns to show in the table, in this order.
    /// @param rows    Data for the table: a map for each row, with the column as the key and the
    ///                content as the value.
    /// @return QueryResult that outputs a table.
    QueryResult table(List<String> columns, List<Map<String, String>> rows);

    enum Alignment
    {
        LEFT,
        RIGHT,
        CENTER
    }

    /// Creates a Markdown table, nicely formatted and columns aligned.
    ///
    /// @param columns    Columns to show in the table, in this order.
    /// @param rows       Data for the table: a map for each row, with the column as the key and the
    ///                   content as the value.
    /// @param alignments Alignment for each of the columms
    /// @return QueryResult that outputs a table.
    QueryResult table(
        List<String> columns, List<Alignment> alignments,
        List<Map<String, String>> rows);

    /// Creates an unordered Markdown list
    ///
    /// @param rows Data for the list; each value is written as is.
    /// @return QueryResult that outputs a list.
    QueryResult unorderedList(List<String> rows);

    QueryResult string(String output);

    QueryResult withPerformanceWarning(QueryResult slowQueryResult);

    QueryResultFactory withPerformanceWarning();
}

package nl.ulso.curator.query;

import java.util.List;
import java.util.Map;

public interface QueryResultFactory
{
    /// @return a [String] result.
    QueryResult string(String output);

    /// @return an empty result; it shows that there are no results.
    QueryResult empty();

    /// Creates an error result; it adds a level-3 "Error" header at the top.
    ///
    /// @param errorMessage The message to show in the error.
    /// @return QueryResult that represents an error.
    QueryResult error(String errorMessage);

    enum Alignment
    {
        LEFT,
        RIGHT,
        CENTER
    }

    /// Creates a table.
    ///
    /// @param columns Columns to show in the table, in this order.
    /// @param rows    Data for the table: a map for each row, with the column as the key and the
    /// content as the value.
    /// @return QueryResult that represents the table.
    QueryResult table(List<String> columns, List<Map<String, String>> rows);

    /// Creates a table including instructions for column alignment..
    ///
    /// @param columns    Columns to show in the table, in this order.
    /// @param alignments Alignment for each of the columms
    /// @param rows       Data for the table: a map for each row, with the column as the key and the
    /// content as the value.
    /// @return QueryResult that represents the table.
    QueryResult table(
        List<String> columns,
        List<Alignment> alignments,
        List<Map<String, String>> rows);

    /// Creates an unordered list
    ///
    /// @param rows Data for the list; each value is written as is.
    /// @return QueryResult that outputs a list.
    QueryResult unorderedList(List<String> rows);
}

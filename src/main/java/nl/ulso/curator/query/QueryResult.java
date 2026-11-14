package nl.ulso.curator.query;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

/// Represents the result of running a query.
///
/// A result contains data that later has to be transformed into a specific output format.
/// To do that, the result is passed to a [QueryResultFormatter].
///
/// This interface offers factory methods for a built-in set of [QueryResult] types that each
/// also have a formatter for [OutputFormat#MARKDOWN] available.
///
/// Modules can provide their own [QueryResult] types combined with one or more
/// [QueryResultFormatter]s for each type.
public interface QueryResult
{
    enum Alignment
    {
        LEFT,
        RIGHT,
        CENTER
    }

    /// @return a [String] result; an empty result if the output is blank.
    static QueryResult string(String output)
    {
        if (output.isBlank())
        {
            return empty();
        }
        return new StringResult(output);
    }

    /// @return an empty result; it shows that there are no results.
    static QueryResult empty()
    {
        return new EmptyResult();
    }

    /// Creates an error result; it adds a level-3 "Error" header at the top.
    ///
    /// @param errorMessage The message to show in the error.
    /// @return QueryResult that represents an error.
    static QueryResult error(String errorMessage)
    {
        return new ErrorResult(errorMessage);
    }

    /// Creates a table.
    ///
    /// @param columns Columns to show in the table, in this order.
    /// @param rows    Data for the table: a map for each row, with the column as the key and the
    ///                content as the value.
    /// @return QueryResult that represents the table.
    static QueryResult table(List<String> columns, List<Map<String, String>> rows)
    {
        return table(
            columns,
            Collections.nCopies(columns.size(), Alignment.LEFT),
            rows
        );
    }

    /// Creates a table including instructions for column alignment.
    ///
    /// @param columns    Columns to show in the table, in this order.
    /// @param alignments Alignment for each of the columns
    /// @param rows       Data for the table: a map for each row, with the column as the key and
    ///                   the content as the value.
    /// @return QueryResult that represents the table.
    static QueryResult table(
        List<String> columns,
        List<Alignment> alignments,
        List<Map<String, String>> rows)
    {
        if (rows.isEmpty())
        {
            return empty();
        }
        return new TableResult(
            unmodifiableList(columns),
            unmodifiableList(alignments),
            unmodifiableList(rows)
        );
    }

    /// Creates an unordered list
    ///
    /// @param rows Data for the list; each value is written as is.
    /// @return QueryResult that outputs a list.
    static QueryResult unorderedList(List<String> rows)
    {
        if (rows.isEmpty())
        {
            return empty();
        }
        return new UnorderedListResult(unmodifiableList(rows));
    }
}

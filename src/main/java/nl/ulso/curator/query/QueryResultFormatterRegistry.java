package nl.ulso.curator.query;

import java.util.Optional;
import java.util.Set;

/// Formats [QueryResult] by finding the appropriate [QueryResultFormatter] for the requested
/// [OutputFormat] and applying it.
///
/// [QueryResultFormatter]s support both a [QueryResult] type and an [OutputFormat]. Given a result,
/// the registry looks for the formatters that support this combination: the provided query result
/// must be assignable (be an "instance of") the supported query result type. If multiple formatters
/// for the same format support a query result, the one with the most specific query result type
/// wins. If that still leaves more than one, statically configured formatters take precedence over
/// dynamically registered ones; beyond that, the effect is undefined.
///
/// Output formats are looked up by name. The set of formats is not fixed: it consists of the
/// formats of all registered formatters, and can change at runtime as extensions are loaded and
/// unloaded.
public interface QueryResultFormatterRegistry
{
    /// @return all formats that at least one formatter supports.
    Set<OutputFormat> outputFormats();

    /// @return the output format with the given name, if any; names are case-insensitive.
    Optional<OutputFormat> outputFormat(String name);

    /// Returns true if this registry has a formatter for the given query result in the given output
    /// format; it doesn't take the fallback to [OutputFormat#MARKDOWN] into account.
    boolean supports(QueryResult queryResult, OutputFormat outputFormat);

    /// Formats the [QueryResult] in the requested [OutputFormat], if possible.
    ///
    /// If the requested format has no formatter for this type of query result, or the format
    /// itself is unknown, the result is formatted as [OutputFormat#MARKDOWN] instead. If that isn't
    /// possible either, the result is an error message in [OutputFormat#MARKDOWN], accompanied by
    /// error messages in the application log. The returned [FormattedResult] has the format the
    /// output is actually in.
    ///
    /// Selecting the formatter and applying it is atomic with respect to formatters being replaced
    /// at runtime.
    FormattedResult formatResult(QueryResult queryResult, OutputFormat outputFormat);

    /// Formats the [QueryResult] as a [String]; see [#formatResult(QueryResult, OutputFormat)].
    default String format(QueryResult queryResult, OutputFormat outputFormat)
    {
        return formatResult(queryResult, outputFormat).output();
    }
}

package nl.ulso.curator.query;

/// Formats [QueryResult] by finding the appropriate [QueryResultFormatter] for the requested
/// [OutputFormat] and applying it.
///
/// [QueryResultFormatter]s support both a [QueryResult] type and an [OutputFormat]. Given a result
/// the registry looks for the first formatter that supports this combination. The provided query
/// result must be assignable (be an "instance of") the supported query result type. If multiple
/// formatters for the same format support a query result, the effect is undefined: one will be
/// chosen arbitrarily.
public interface QueryResultFormatterRegistry
{
    /// Formats the [QueryResult] as a [String].
    ///
    /// This method does not throw any exception. If no matching formatter can be found, the result
    /// is [String] error message, accompanied by error messages in the application log.
    String format(QueryResult queryResult, OutputFormat outputFormat);
}

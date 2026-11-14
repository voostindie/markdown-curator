package nl.ulso.curator.query;

/// Output of a formatted [QueryResult], together with the [OutputFormat] it is actually in. That
/// format can differ from the requested one: if a format doesn't support a specific type of query
/// result, the result is formatted as [OutputFormat#MARKDOWN] instead.
///
/// @param outputFormat the format the output is in.
/// @param output       the formatted query result.
public record FormattedResult(OutputFormat outputFormat, String output)
{
}

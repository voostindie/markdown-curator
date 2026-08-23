package nl.ulso.curator.query;

/// Represents the result of running a query.
///
/// A result contains data that later has to be transformed into a specific output format.
/// To do that, the result is passed to a [QueryResultFormatter].
///
/// The [QueryResultFactory] offers a built-in set of [QueryResult] types that each also have
/// a formatter for [OutputFormat#MARKDOWN] available.
///
/// Modules can provide their own [QueryResult] types combined with one or more
/// [QueryResultFormatter]s for each type.
public interface QueryResult
{
}

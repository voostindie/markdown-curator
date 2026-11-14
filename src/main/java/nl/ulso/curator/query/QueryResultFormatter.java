package nl.ulso.curator.query;

import dagger.Binds;
import dagger.multibindings.IntoSet;

/// Formats a [QueryResult] as a [String] for a specific [OutputFormat].
///
/// This implementation uses generics so that implementors can directly access members of the
/// specific [QueryResult] type in their [#format(QueryResult)] method.
///
/// To add a formatter to the system, [Binds] [IntoSet] of [QueryResultFormatter<?>]. Extensions
/// provide their formatters through [nl.ulso.curator.extension.Extension#queryResultFormatters()]
/// instead; a formatter with a new [OutputFormat] introduces that format.
public interface QueryResultFormatter<T extends QueryResult>
{
    /// [QueryResult] type that this formatter can format.
    Class<T> queryResultType();

    /// [OutputFormat] that this formatter produces.
    OutputFormat outputFormat();

    /// Formats the given [QueryResult] as a [String] for the given [OutputFormat].
    String format(T result);
}

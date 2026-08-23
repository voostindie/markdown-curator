package nl.ulso.curator.query;

import dagger.Binds;
import dagger.multibindings.IntoSet;

/// Formats a [QueryResult] as a [String] for a specific [OutputFormat].
///
/// This implementation uses generics so that implementors can directly access members of the
/// specific [QueryResult] type in their [#format(QueryResult)] method.
///
/// To add a formatter to the system, [Binds] [IntoSet] of [QueryResultFormatter<?>].
public interface QueryResultFormatter<QR extends QueryResult>
{
    /// [QueryResult] type that this formatter can format.
    Class<QR> queryResultType();

    /// [OutputFormat] that this formatter produces.
    OutputFormat outputFormat();

    /// Formats the given [QueryResult] as a [String] for the given [OutputFormat].
    String format(QR result);
}

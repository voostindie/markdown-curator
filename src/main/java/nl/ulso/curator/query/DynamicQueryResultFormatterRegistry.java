package nl.ulso.curator.query;

import java.util.Collection;

/// [QueryResultFormatterRegistry] that can have formatters added and removed at runtime, next to
/// the formatters that are statically configured.
public interface DynamicQueryResultFormatterRegistry
    extends QueryResultFormatterRegistry
{
    /// Replaces all dynamically registered formatters with the given formatters. The replacement is
    /// atomic: readers see either the old or the new set of formatters, never a mix.
    ///
    /// @param formatters the formatters to register; replaces all previously registered dynamic
    ///                   formatters.
    void replaceDynamicFormatters(Collection<QueryResultFormatter<?>> formatters);
}

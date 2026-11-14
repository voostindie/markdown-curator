package nl.ulso.curator.query;

import java.util.Collection;
import java.util.Set;

/// [QueryResultFormatterRegistry] that can have formatters added and removed at runtime, next to
/// the formatters that are statically configured.
public interface DynamicQueryResultFormatterRegistry
    extends QueryResultFormatterRegistry
{
    /// @return the output formats of the statically configured formatters; these never change.
    Set<OutputFormat> staticOutputFormats();

    /// Replaces all dynamically registered formatters with the given formatters. The replacement is
    /// atomic: readers see either the old or the new set of formatters, never a mix.
    ///
    /// A formatter whose output format has the same name as an already registered format, but
    /// differs from it, is ignored, with a warning in the log. Callers are expected to prevent
    /// such conflicts up front.
    ///
    /// @param formatters the formatters to register; replaces all previously registered dynamic
    ///                   formatters.
    void replaceDynamicFormatters(Collection<QueryResultFormatter<?>> formatters);
}

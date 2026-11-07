package nl.ulso.curator.query;

import java.util.Collection;
import java.util.Set;

/// [QueryCatalog] that can have queries added and removed at runtime, next to the queries that
/// are statically configured.
public interface DynamicQueryCatalog
    extends QueryCatalog
{
    /// Replaces all dynamically registered queries with the given queries. The replacement is
    /// atomic: readers see either the old or the new set of queries, never a mix.
    ///
    /// @param queries the queries to register; replaces all previously registered dynamic queries.
    /// @throws IllegalArgumentException if any of the queries has a name that is already in use
    ///                                  by a static query or by another query in the collection.
    ///                                  The catalog is then left unchanged.
    void replaceDynamicQueries(Collection<Query> queries);

    /// @return the names of all statically configured queries, including built-in ones.
    Set<String> staticQueryNames();
}

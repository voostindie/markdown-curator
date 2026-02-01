package nl.ulso.curator.query;

import java.util.Collection;

/// Manages all queries available for a vault.
///
/// Requesting a non-existing query does not result in an error, but in a query that, when run,
/// results in a descriptive error message that can be written as normal output.
public interface QueryCatalog
{
    /// @return `true` if this catalog has queries registered, `false` otherwise.
    boolean isEmpty();

    /// @return All queries in this catalog.
    Collection<Query> queries();

    /// @param name Name of the query to get from the catalog.
    /// @return The query with the specified name, or a default query if no such query exists.
    Query query(String name);
}

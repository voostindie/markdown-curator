package nl.ulso.obsidian.watcher.vault;

import java.util.*;

/**
 * Finds all queries in all documents
 */
final class QueryFinder
        extends BreadthFirstVaultVisitor
{
    private final Map<Query, Location> queries = new HashMap<>();

    @Override
    public void visit(Query query)
    {
        queries.put(query, currentLocation());
    }

    Map<Query, Location> queries()
    {
        return Collections.unmodifiableMap(queries);
    }
}

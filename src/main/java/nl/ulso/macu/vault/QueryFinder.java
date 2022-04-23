package nl.ulso.macu.vault;

import java.util.*;

/**
 * Finds all queries in all documents
 */
final class QueryFinder
        extends BreadthFirstVaultVisitor
{
    private final List<QueryBlock> queries = new ArrayList<>();

    @Override
    public void visit(QueryBlock queryBlock)
    {
        queries.add(queryBlock);
    }

    Collection<QueryBlock> queries()
    {
        return Collections.unmodifiableCollection(queries);
    }
}

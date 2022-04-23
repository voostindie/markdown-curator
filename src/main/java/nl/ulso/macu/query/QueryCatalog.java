package nl.ulso.macu.query;

import java.util.Collection;

public interface QueryCatalog
{
    /**
     * Registers the query in the catalog. If a query with the same name already exists, it is
     * overwritten.
     *
     * @param query Query to register.
     */
    void register(Query query);

    /**
     * @return All queries in this catalog.
     */
    Collection<Query> queries();

    /**
     * @param name Name of the query to get from the catalog.
     * @return The query with the specified name, or a default query if no such query exists.
     */
    Query query(String name);
}

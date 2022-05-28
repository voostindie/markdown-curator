package nl.ulso.markdown_curator.query;

import nl.ulso.markdown_curator.query.builtin.HelpQuery;
import nl.ulso.markdown_curator.query.builtin.UnknownQuery;

import java.util.*;

import static java.util.Collections.unmodifiableCollection;

/**
 * Simple {@link QueryCatalog} implementation that keeps the catalog in memory.
 */
public class InMemoryQueryCatalog
        implements QueryCatalog
{
    private final Map<String, Query> queries;

    public InMemoryQueryCatalog()
    {
        this.queries = new HashMap<>();
        register(new HelpQuery(this));
    }

    @Override
    public void register(Query query)
    {
        queries.put(query.name(), query);
    }

    @Override
    public boolean isEmpty()
    {
        return queries.size() == 1;
    }

    @Override
    public Collection<Query> queries()
    {
        return unmodifiableCollection(queries.values());
    }

    @Override
    public Query query(String name)
    {
        return queries.getOrDefault(name, new UnknownQuery(this, name));
    }
}

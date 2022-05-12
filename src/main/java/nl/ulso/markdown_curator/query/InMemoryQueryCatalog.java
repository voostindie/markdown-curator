package nl.ulso.markdown_curator.query;

import java.util.*;

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
    public Collection<Query> queries()
    {
        return Collections.unmodifiableCollection(queries.values());
    }

    @Override
    public Query query(String name)
    {
        return queries.getOrDefault(name, new UnknownQuery(this, name));
    }
}

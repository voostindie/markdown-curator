package nl.ulso.curator.main;

import nl.ulso.curator.query.DynamicQueryCatalog;
import nl.ulso.curator.query.Query;

import java.util.*;

/// Minimal in-memory [DynamicQueryCatalog] for tests that don't need the full query module.
public final class DynamicQueryCatalogStub
    implements DynamicQueryCatalog
{
    private final Set<String> staticQueryNames;
    private Map<String, Query> dynamicQueries = Map.of();

    public DynamicQueryCatalogStub(String... staticQueryNames)
    {
        this.staticQueryNames = Set.of(staticQueryNames);
    }

    @Override
    public void replaceDynamicQueries(Collection<Query> queries)
    {
        var map = new HashMap<String, Query>();
        for (Query query : queries)
        {
            if (staticQueryNames.contains(query.name()) ||
                map.putIfAbsent(query.name(), query) != null)
            {
                throw new IllegalArgumentException("Duplicate query name: " + query.name());
            }
        }
        dynamicQueries = Map.copyOf(map);
    }

    @Override
    public Set<String> staticQueryNames()
    {
        return staticQueryNames;
    }

    @Override
    public boolean isEmpty()
    {
        return dynamicQueries.isEmpty();
    }

    @Override
    public Collection<Query> queries()
    {
        return dynamicQueries.values();
    }

    @Override
    public Query query(String name)
    {
        var query = dynamicQueries.get(name);
        if (query == null)
        {
            throw new NoSuchElementException(name);
        }
        return query;
    }
}

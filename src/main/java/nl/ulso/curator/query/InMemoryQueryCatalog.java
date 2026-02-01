package nl.ulso.curator.query;

import nl.ulso.curator.query.builtin.HelpQuery;
import nl.ulso.curator.query.builtin.UnknownQuery;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.*;

import static java.util.Collections.unmodifiableMap;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple {@link QueryCatalog} implementation that keeps the catalog in memory.
 * <p/>
 * o The queries kept in memory are injected by Guice, which collects all available queries in a
 * single Set. This implementations turns that set into a map for easier reference, logging
 * duplicates as they are found.
 */
@Singleton
public class InMemoryQueryCatalog
        implements QueryCatalog
{
    private static final Logger LOGGER = getLogger(InMemoryQueryCatalog.class);

    private final Map<String, Query> queries;
    private final QueryResultFactory resultFactory;

    @Inject
    public InMemoryQueryCatalog(Set<Query> querySet, QueryResultFactory resultFactory)
    {
        this.resultFactory = resultFactory;
        Map<String, Query> map = new HashMap<>();
        var help = new HelpQuery(this);
        map.put(help.name(), help);
        for (Query query : querySet)
        {
            if (!map.containsKey(query.name()))
            {
                map.put(query.name(), query);
            }
            else if (LOGGER.isWarnEnabled())
            {
                LOGGER.warn("Duplicate query name: '{}'. Application behavior is unspecified!",
                        query.name());
            }
        }
        queries = unmodifiableMap(map);
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Initialized query catalog with {} queries", queries.size());
        }
    }

    @Override
    public boolean isEmpty()
    {
        return queries.size() == 1;
    }

    @Override
    public Collection<Query> queries()
    {
        return queries.values();
    }

    @Override
    public Query query(String name)
    {
        var query = queries.get(name);
        if (query != null)
        {
            return query;
        }
        return new UnknownQuery(this, name, resultFactory);
    }
}

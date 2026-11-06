package nl.ulso.curator.query;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;

import java.util.*;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

/// Simple [QueryCatalog] implementation that keeps the catalog in memory.
///
/// The static queries kept in memory are injected by Dagger, which collects all available queries
/// in a single Set. This implementation turns that set into a map for easier reference, logging
/// duplicates as they are found.
///
/// On top of that, queries can be registered at runtime. These dynamic queries can be replaced as a
/// whole. Because queries are looked up concurrently - by the query orchestrator and by the HTTP
/// server - the catalog is never modified in place: it's replaced by a new, immutable map.
@Singleton
final class DefaultQueryCatalog
    implements DynamicQueryCatalog
{
    private static final Logger LOGGER = getLogger(DefaultQueryCatalog.class);

    private final Map<String, Query> staticQueries;
    private final DefaultQueryResultFactory resultFactory;
    private volatile Map<String, Query> queries;

    @Inject
    DefaultQueryCatalog(Set<Query> querySet, DefaultQueryResultFactory resultFactory)
    {
        this.resultFactory = resultFactory;
        Map<String, Query> map = new HashMap<>();
        var help = new HelpQuery(this, resultFactory);
        map.put(help.name(), help);
        for (Query query : querySet)
        {
            if (!map.containsKey(query.name()))
            {
                map.put(query.name(), query);
            }
            else if (LOGGER.isWarnEnabled())
            {
                LOGGER.warn(
                    "Duplicate query name: '{}'. Application behavior is unspecified!",
                    query.name()
                );
            }
        }
        staticQueries = unmodifiableMap(map);
        queries = staticQueries;
        logAvailableQueries();
    }

    @Override
    public void replaceDynamicQueries(Collection<Query> dynamicQueries)
    {
        var map = new HashMap<>(staticQueries);
        for (Query query : dynamicQueries)
        {
            if (map.putIfAbsent(query.name(), query) != null)
            {
                throw new IllegalArgumentException(
                    "Duplicate query name: '" + query.name() + "'. Queries not registered.");
            }
        }
        queries = unmodifiableMap(map);
        logAvailableQueries();
    }

    private void logAvailableQueries()
    {
        if (LOGGER.isDebugEnabled())
        {
            var snapshot = queries;
            LOGGER.debug("{} queries are available in the catalog: {}.",
                snapshot.size(),
                snapshot.keySet().stream()
                    .sorted()
                    .map(query -> "'" + query + "'")
                    .collect(joining(", "))
            );
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

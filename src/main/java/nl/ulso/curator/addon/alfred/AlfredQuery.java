package nl.ulso.curator.addon.alfred;

import jakarta.inject.Inject;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;

import java.util.*;

/// Exposes settings for the Alfred workflow
final class AlfredQuery
    implements Query
{
    private final AlfredSettings settings;
    private final QueryResultFactory queryResultFactory;

    @Inject
    AlfredQuery(AlfredSettings settings, QueryResultFactory queryResultFactory)
    {
        this.settings = settings;
        this.queryResultFactory = queryResultFactory;
    }

    @Override
    public String name()
    {
        return "alfred";
    }

    @Override
    public String description()
    {
        return "Output settings for the Alfred workflow";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Collections.emptyMap();
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return false;
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        return queryResultFactory.table(
            List.of("name", "value"),
            List.of(
                Map.of("name", "projects",
                    "value", settings.projectFolder()
                )
            )
        );
    }
}

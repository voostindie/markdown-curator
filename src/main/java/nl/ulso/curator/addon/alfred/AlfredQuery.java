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
        var features = new ArrayList<Map<String, String>>();
        addTo(features, "contacts", settings.contactsPath());
        addTo(features, "teams", settings.teamsPath());
        addTo(features, "third-parties", settings.thirdPartiesPath());
        addTo(features, "projects", settings.projectsPath());
        addTo(features, "reminders", settings.remindersPath());
        addTo(features, "systems", settings.systemsPath());
        return queryResultFactory.table(
            List.of("name", "value"),
            features
        );
    }

    private void addTo(ArrayList<Map<String, String>> features, String name, String value)
    {
        if (value != null)
        {
            features.add(Map.of("name", name, "value", value));
        }
    }
}

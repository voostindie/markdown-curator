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
        settings.projectsPath().ifPresent(path -> features.add(
            Map.of("name", "projects", "value", path)
        ));
        settings.contactsPath().ifPresent(path -> features.add(
            Map.of("name", "contacts", "value", path)
        ));
        settings.teamsPath().ifPresent(path -> features.add(
            Map.of("name", "teams", "value", path)
        ));
        settings.thirdPartiesPath().ifPresent(path -> features.add(
            Map.of("name", "third-parties", "value", path)
        ));
        settings.remindersPath().ifPresent(path -> features.add(
            Map.of("name", "reminders", "value", path)
        ));
        return queryResultFactory.table(
            List.of("name", "value"),
            features
        );
    }
}

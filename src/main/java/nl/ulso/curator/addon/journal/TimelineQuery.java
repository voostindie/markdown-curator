package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.newday.NewDayAlarm;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;

import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static nl.ulso.curator.change.Change.isPayloadType;

/// Generates a timeline for a document, extracted from the daily logs, Logseq-style.
@Singleton
public class TimelineQuery
    implements Query
{
    private final Journal journal;
    private final NewDayAlarm newDayAlarm;
    private final QueryResultFactory resultFactory;

    @Inject
    TimelineQuery(Journal journal, NewDayAlarm newDayAlarm, QueryResultFactory resultFactory)
    {
        this.journal = journal;
        this.newDayAlarm = newDayAlarm;
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return "timeline";
    }

    @Override
    public String description()
    {
        return "Generates a timeline for the selected document, " +
               "with entries extracted from the journal, newest entry first";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("document", "Name of the document; defaults to the current document",
            "limit", "Number of days from today to include; defaults to all available"
        );
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        var documentName = resolveDocumentName(definition);
        var daysFromToday = resolveDaysFromToday(definition);
        return changelog.changes()
                   .anyMatch(isPayloadType(Daily.class).and(change ->
                       change.as(Daily.class).values()
                           .anyMatch(daily -> daily.refersTo(documentName)))
                   ) || (daysFromToday > 0 && newDayAlarm.didAlarmTrigger(changelog));
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var documentName = resolveDocumentName(definition);
        var daysFromToday = resolveDaysFromToday(definition);
        var timeline = journal.timelineFor(documentName, daysFromToday);
        var result = timeline.entrySet().stream()
            .map(entry -> "- **[[" + entry.getKey() + "]]**:" + lineSeparator() +
                          entry.getValue().indent(4))
            .collect(joining());
        return resultFactory.string(result + lineSeparator());
    }

    private String resolveDocumentName(QueryDefinition definition)
    {
        return definition.configuration().string("document", definition.document().name());
    }

    private int resolveDaysFromToday(QueryDefinition definition)
    {
        return definition.configuration().integer("limit", -1);
    }
}

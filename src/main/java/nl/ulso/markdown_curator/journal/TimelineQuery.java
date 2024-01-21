package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.*;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

/**
 * Generates a timeline for a document, extracted from the daily logs, Logseq-style.
 */
@Singleton
public class TimelineQuery
        implements Query
{
    private final Journal journal;
    private final QueryResultFactory resultFactory;

    @Inject
    TimelineQuery(Journal journal, QueryResultFactory resultFactory)
    {
        this.journal = journal;
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
                "limit", "Maximum number of entries to include; defaults to all");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var documentName =
                definition.configuration().string("document", definition.document().name());
        var limit = definition.configuration().integer("limit", -1);
        var timeline = journal.timelineFor(documentName);
        var stream = timeline.entrySet().stream();
        if (limit > 0)
        {
            stream = stream.limit(limit);
        }
        var result = stream
                .map(entry -> "- **[[" + entry.getKey() + "]]**:" + lineSeparator() +
                              entry.getValue().indent(4))
                .collect(joining());
        return resultFactory.string(result + lineSeparator());
    }
}

package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.*;

import javax.inject.Inject;
import javax.inject.Singleton;
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
    private final Journal model;
    private final QueryResultFactory resultFactory;

    @Inject
    TimelineQuery(Journal model, QueryResultFactory resultFactory)
    {
        this.model = model;
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
        return Map.of("document", "Name of the document; defaults to the current document");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var documentName =
                definition.configuration().string("document", definition.document().name());
        var timeline = model.timelineFor(documentName);
        var result = timeline.entrySet().stream()
                .map(entry -> "- **[[" + entry.getKey() + "]]**:" + lineSeparator() +
                              entry.getValue().indent(4))
                .collect(joining());
        return resultFactory.stringWithSummary(result, timeline.size());
    }
}

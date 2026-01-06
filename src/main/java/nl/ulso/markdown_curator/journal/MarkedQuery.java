package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.query.*;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static nl.ulso.markdown_curator.Change.isObjectType;

/**
 * Generates an overview of marked lines for a document, one section per marker
 */
@Singleton
public class MarkedQuery
        implements Query
{
    private final Journal journal;
    private final QueryResultFactory resultFactory;

    @Inject
    MarkedQuery(Journal journal, QueryResultFactory resultFactory)
    {
        this.journal = journal;
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return "marked";
    }

    @Override
    public String description()
    {
        return "Generates an overview of the marked lines for the selected document, with entries" +
               " extracted from the journal. Each marker gets its own section. The title of the " +
               "section defaults to the name of the marker, but this can be overruled by setting " +
               "the 'title' property of the marker document itself (if present). If a marker is " +
               "not present for the selected document, the section is left out.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("document", "Name of the document; defaults to the current document",
                "markers", "List of markers to include in the overview.",
                "level", "The level of the sections to use, defaults to 2");
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return changelog.changes()
            .anyMatch(isObjectType(Daily.class).and(change ->
                change.objectAs(Daily.class).refersTo(resolveDocumentName(definition))));
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var documentName = resolveDocumentName(definition);
        var markers = new LinkedHashSet<>(definition.configuration().listOfStrings("markers"));
        var level = "#".repeat(definition.configuration().integer("level", 2));
        var markedLines = journal.markedLinesFor(documentName, markers);
        var result = new StringBuilder();
        for (var markerName : markers) // Retain the order of the markers in the configuration
        {
            var lines = markedLines.get(markerName);
            if (lines == null)
            {
                continue;
            }
            var markerSettings = journal.markerSettings(markerName);
            result.append(level)
                    .append(" ")
                    .append(markerSettings.string("title", markerName))
                    .append(lineSeparator())
                    .append(lineSeparator());
            var groupByDate = markerSettings.bool("group-by-date", false);
            LocalDate currentDate = null;
            for (var line : lines)
            {
                if (groupByDate)
                {
                    if (!line.date().equals(currentDate))
                    {
                        currentDate = line.date();
                        result.append("- [[")
                                .append(currentDate)
                                .append("]]:")
                                .append(lineSeparator());
                    }
                    result.append("    ");
                }
                result.append(line.line()).append(lineSeparator());
            }
            result.append(lineSeparator());
        }
        return resultFactory.string(result.toString());
    }

    private String resolveDocumentName(QueryDefinition definition)
    {
        return definition.configuration().string("document", definition.document().name());
    }
}

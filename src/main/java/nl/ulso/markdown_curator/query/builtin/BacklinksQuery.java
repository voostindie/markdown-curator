package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.InternalLink;
import nl.ulso.markdown_curator.vault.Section;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static nl.ulso.markdown_curator.query.QueryResult.empty;
import static nl.ulso.markdown_curator.vault.Section.createAnchor;

public final class BacklinksQuery
        implements Query
{
    private final LinksModel linksModel;
    private static final String DOCUMENT_PROPERTY = "document";

    @Inject
    public BacklinksQuery(LinksModel linksModel)
    {
        this.linksModel = linksModel;
    }

    @Override
    public String name()
    {
        return "backlinks";
    }

    @Override
    public String description()
    {
        return "Lists all backlinks to a document";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(DOCUMENT_PROPERTY,
                "Name of the document to list backlinks for; defaults to the current document");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var documentName = definition.configuration()
                .string(DOCUMENT_PROPERTY, definition.document().name());
        var links = linksModel.incomingLinksFor(documentName).stream()
                .map(this::convertLink)
                .distinct()
                .collect(groupingBy(InternalLink::targetDocument));
        if (links.isEmpty())
        {
            return empty();
        }
        return () ->
        {
            var builder = new StringBuilder();
            links.keySet().stream().sorted().forEach(document ->
            {
                builder.append("- [[").append(document).append("]]").append(lineSeparator());
                var line = links.get(document).stream()
                        .filter(link -> link.targetAnchor().isPresent())
                        .map(InternalLink::toMarkdown)
                        .collect(joining(", "));
                if (!line.isEmpty())
                {
                    builder.append("    - ").append(line).append(lineSeparator());
                }
            });
            return builder.toString();
        };
    }

    private InternalLink convertLink(InternalLink link)
    {
        var fragment = link.sourceLocation();
        if (fragment instanceof Section section)
        {
            return new InternalLink(
                    null,
                    fragment.document().name(),
                    Optional.of(section.anchor()),
                    Optional.of(createAnchor(section.title())));
        }
        else
        {
            var parentSection = fragment.parentSection();
            return new InternalLink(
                    null,
                    fragment.document().name(),
                    parentSection.map(Section::anchor),
                    parentSection.map(section -> createAnchor(section.title())));
        }
    }
}

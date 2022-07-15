package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.*;

import javax.inject.Inject;
import java.util.Map;

import static nl.ulso.markdown_curator.query.QueryResult.empty;
import static nl.ulso.markdown_curator.query.QueryResult.unorderedList;

public final class DeadLinksQuery
        implements Query
{
    private final LinksModel linksModel;
    private static final String DOCUMENT_PROPERTY = "document";

    @Inject
    public DeadLinksQuery(LinksModel linksModel)
    {
        this.linksModel = linksModel;
    }

    @Override
    public String name()
    {
        return "deadlinks";
    }

    @Override
    public String description()
    {
        return "Lists all dead links from a document";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(DOCUMENT_PROPERTY,
                "Name of the document to list dead links for; defaults to the current document");
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var documentName = definition.configuration()
                .string(DOCUMENT_PROPERTY, definition.document().name());
        var links = linksModel.deadLinksFor(documentName);
        if (links.isEmpty())
        {
            return empty();
        }
        return unorderedList(links.stream().map(document -> "[[" + document + "]]").toList());
    }
}

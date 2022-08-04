package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.*;

import javax.inject.Inject;
import java.util.Map;

public final class DeadLinksQuery
        implements Query
{
    private final LinksModel linksModel;
    private final QueryResultFactory resultFactory;
    private static final String DOCUMENT_PROPERTY = "document";

    @Inject
    public DeadLinksQuery(LinksModel linksModel, QueryResultFactory resultFactory)
    {
        this.linksModel = linksModel;
        this.resultFactory = resultFactory;
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
        return resultFactory.unorderedList(
                linksModel.deadLinksFor(documentName).stream()
                        .map(document -> "[[" + document + "]]")
                        .toList());
    }
}

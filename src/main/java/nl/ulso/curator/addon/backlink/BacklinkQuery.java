package nl.ulso.curator.addon.backlink;

import jakarta.inject.Inject;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.extension.ExtensionScope;
import nl.ulso.curator.query.*;
import nl.ulso.emoji.EmojiStripper;

import java.util.Map;

import static java.util.Comparator.comparing;
import static nl.ulso.curator.query.QueryResult.unorderedList;

@ExtensionScope
final class BacklinkQuery
    implements Query
{
    static final String BACKLINK_QUERY_NAME = "backlinks";
    private static final String DOCUMENT_PROPERTY = "document";

    private final BacklinkRepository repository;

    @Inject
    BacklinkQuery(BacklinkRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public String name()
    {
        return BACKLINK_QUERY_NAME;
    }

    static String resolveDocumentName(QueryDefinition definition)
    {
        return definition.configuration().string(
            DOCUMENT_PROPERTY,
            definition.document().name()
        );
    }

    @Override
    public String description()
    {
        return "Generates a list of documents that link to the specified document";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
            DOCUMENT_PROPERTY,
            "The document to generate backlinks for; defaults to the current document"
        );
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        var targetDocumentName = resolveDocumentName(definition);
        return changelog.changesFor(Backlink.class)
            .anyMatch(change ->
                change.value().targetDocumentName().contentEquals(targetDocumentName)
            );
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var targetDocumentName = resolveDocumentName(definition);
        var links = repository.backlinksFor(targetDocumentName).stream()
            .sorted(comparing(EmojiStripper::stripEmojisFrom))
            .map(documentName -> "[[" + documentName + "]]")
            .toList();
        return unorderedList(links);
    }
}

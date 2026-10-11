package nl.ulso.curator.addon.alfred;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.addon.journal.Marker;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.compile;

/// Gathers all external links for a document, combining links in specific links sections with any
/// links from the timeline marked by a specific marker.
///
/// Links are extracted from one section in each document. The default name is "Links". It can be
/// overridden by providing a `@Named String("documentLinkSectionName")`.
///
/// Links are extracted for those markers that have their `export-links` property set to `true`.
final class ExternalLinksQuery
    implements Query
{
    private static final String EXPORT_LINKS_MARKER_PROPERTY = "export-links";
    private static final String DEFAULT_DOCUMENT_LINK_SECTION_NAME = "Links";
    private static final Pattern LINK_PATTERN = compile("\\[([^]]+)]\\(([^)]+)\\)");
    private static final String LINK_NAME = "name";
    private static final String LINK_URL = "url";

    private final Vault vault;
    private final Journal journal;
    private final String documentLinkSectionName;
    private final QueryResultFactory queryResultFactory;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    ExternalLinksQuery(
        Vault vault,
        Journal journal,
        @Named("documentLinkSectionName") Optional<String> documentLinkSectionName,
        QueryResultFactory queryResultFactory)
    {
        this.vault = vault;
        this.journal = journal;
        this.documentLinkSectionName = documentLinkSectionName.orElse(
            DEFAULT_DOCUMENT_LINK_SECTION_NAME);
        this.queryResultFactory = queryResultFactory;
    }

    @Override
    public String name()
    {
        return "links";
    }

    @Override
    public String description()
    {
        return "Gathers all external links for a document, for use in the HTTP server only.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("document", "Document to gather links for.");
    }

    /// This query does not check the changelog and always returns `false`; it's meant to be used in
    /// the HTTP server only.
    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return false;
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var documentName = definition.configuration().string("document", null);
        if (documentName == null)
        {
            return queryResultFactory.error("No document name specified");
        }
        return vault.findDocument(documentName).map(document ->
        {
            List<Link> links = new ArrayList<>();
            collectDocumentLinks(document, links);
            collectJournalLinks(document, links);
            return queryResultFactory.table(
                List.of(LINK_NAME, LINK_URL),
                links.stream().map(Link::toMap).toList()
            );
        }).orElseGet(queryResultFactory::empty);
    }

    private void collectDocumentLinks(Document document, Collection<Link> links)
    {
        var finder = new BreadthFirstVaultVisitor()
        {
            private boolean inLinkSection = false;

            @Override
            public void visit(Section section)
            {
                if (section.level() == 2 &&
                    section.sortableTitle().contentEquals(documentLinkSectionName))
                {
                    inLinkSection = true;
                    super.visit(section);
                    inLinkSection = false;
                }
                else
                {
                    super.visit(section);
                }
            }

            @Override
            public void visit(TextBlock textBlock)
            {
                if (!inLinkSection)
                {
                    return;
                }
                textBlock.markdown().lines()
                    .forEach(line -> collectExternalLinksFromLine(line, links));
            }
        };
        document.accept(finder);
    }

    private void collectJournalLinks(Document document, Collection<Link> links)
    {
        var markers = journal.markers().values().stream()
            .filter(marker -> marker.settings().bool(EXPORT_LINKS_MARKER_PROPERTY, false))
            .map(Marker::name)
            .collect(Collectors.toSet());
        collectExternalLinksFromJournal(document, markers, links);
    }

    private void collectExternalLinksFromJournal(
        Document document, Set<String> markerNames, Collection<Link> links)
    {
        journal.markedLinesFor(document.name(), markerNames)
            .values()
            .forEach(list ->
                list.forEach(
                    markedLine -> collectExternalLinksFromLine(markedLine.line(), links)
                )
            );
    }

    private void collectExternalLinksFromLine(String line, Collection<Link> links)
    {
        LINK_PATTERN.matcher(line).results().forEach(match ->
        {
            var name = match.group(1);
            var url = match.group(2);
            links.add(new Link(name, url));
        });
    }

    private record Link(String name, String url)
    {
        Map<String, String> toMap()
        {
            return Map.of(
                LINK_NAME, name,
                LINK_URL, url
            );
        }
    }

}

package nl.ulso.curator.addon.alfred;

import jakarta.inject.Inject;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.*;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.regex.Pattern.compile;

/// Gathers all external links for a document, combining links in specific links sections with any
/// links from the timeline marked by a specific marker.
///
/// TODO: refactor this into a clean implementation.
final class ExternalLinksQuery
    implements Query
{
    private static final Pattern LINK_PATTERN = compile("\\[([^]]+)]\\(([^)]+)\\)");
    private static final String LINK_NAME = "name";
    private static final String LINK_URL = "url";

    private final Vault vault;
    private final Journal journal;
    private final ExternalLinksQuerySettings settings;
    private final QueryResultFactory queryResultFactory;

    @Inject
    ExternalLinksQuery(
        Vault vault,
        Journal journal,
        ExternalLinksQuerySettings settings,
        QueryResultFactory queryResultFactory)
    {
        this.vault = vault;
        this.journal = journal;
        this.settings = settings;
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
        return "Gathers all external links for a document, for use in the HTTP server only";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("document", "Document to gather links for.");
    }

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
            List<Map<String, String>> links = new ArrayList<>();
            var sectionName = settings.documentSectionName();
            if (sectionName != null)
            {
                collectExternalLinksFromDocument(links, document);
            }
            var markerNames = settings.journalMarkerNames();
            if (markerNames != null && !markerNames.isEmpty())
            {
                collectExternalLinksFromJournal(links, document, markerNames);
            }
            return queryResultFactory.table(
                List.of(LINK_NAME, LINK_URL),
                links
            );
        }).orElseGet(() -> queryResultFactory.table(List.of(LINK_NAME, LINK_URL), emptyList()));
    }

    private void collectExternalLinksFromDocument(
        List<Map<String, String>> links, Document document)
    {
        var finder = new ExternalLinksFinder(links);
        document.accept(finder);
    }

    private void collectExternalLinksFromJournal(
        List<Map<String, String>> links, Document document, Set<String> markerNames)
    {
        journal.markedLinesFor(document.name(), markerNames)
            .values()
            .forEach(list ->
                list.forEach(
                    markedLine -> collectExternalLinksFromLine(markedLine.line(), links)
                )
            );
    }

    private void collectExternalLinksFromLine(String line, List<Map<String, String>> links)
    {
        LINK_PATTERN.matcher(line).results()
            .forEach(match ->
            {
                var name = match.group(1);
                var url = match.group(2);
                links.add(Map.of(LINK_NAME, name, LINK_URL, url));
            });
    }

    private class ExternalLinksFinder
        extends BreadthFirstVaultVisitor
    {
        private final List<Map<String, String>> links;

        public ExternalLinksFinder(List<Map<String, String>> links)
        {
            this.links = links;
        }

        @Override
        public void visit(Section section)
        {
            if (section.level() == 1)
            {
                super.visit(section);
            }
            if (section.level() == 2 &&
                section.sortableTitle().contentEquals(settings.documentSectionName()))
            {
                super.visit(section);
            }
        }

        @Override
        public void visit(TextBlock textBlock)
        {
            textBlock.markdown().lines()
                .forEach(line -> collectExternalLinksFromLine(line, links));
        }
    }
}

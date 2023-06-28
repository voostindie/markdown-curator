package nl.ulso.markdown_curator.links;

import nl.ulso.markdown_curator.DataModelTemplate;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.event.*;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.*;

import static java.util.Collections.emptyList;

@Singleton
public final class LinksModel
        extends DataModelTemplate
{
    private final Vault vault;
    private final Set<String> documents;
    private final Map<String, List<InternalLink>> outgoingLinks;
    private final Map<String, List<InternalLink>> incomingLinks;

    @Inject
    public LinksModel(Vault vault)
    {
        this.vault = vault;
        documents = new HashSet<>();
        outgoingLinks = new HashMap<>();
        incomingLinks = new HashMap<>();
    }

    List<InternalLink> incomingLinksFor(String documentName)
    {
        return incomingLinks.getOrDefault(documentName, emptyList());
    }

    List<String> deadLinksFor(String documentName)
    {
        return outgoingLinks.getOrDefault(documentName, emptyList()).stream()
                .map(InternalLink::targetDocument)
                .filter(document -> !document.isBlank())
                .filter(document -> !documents.contains(document))
                .sorted()
                .distinct()
                .toList();
    }

    @Override
    protected void fullRefresh()
    {
        documents.clear();
        incomingLinks.clear();
        outgoingLinks.clear();
        processLinks(vault);
    }

    @Override
    public void process(DocumentAdded event)
    {
        processLinks(event.document());
    }

    @Override
    public void process(DocumentChanged event)
    {
        outgoingLinks.remove(event.document().name());
        processLinks(event.document());
    }

    @Override
    public void process(DocumentRemoved event)
    {
        var documentName = event.document().name();
        documents.remove(documentName);
        outgoingLinks.remove(documentName);
    }

    @Override
    public void process(FolderAdded event)
    {
        processLinks(event.folder());
    }

    @Override
    public void process(FolderRemoved event)
    {
        var finder = new Finder();
        event.folder().accept(finder);
        for (String document : finder.documents)
        {
            documents.remove(document);
            outgoingLinks.remove(document);
        }
    }

    private void processLinks(Visitable visitable)
    {
        var finder = new Finder();
        visitable.accept(finder);
        documents.addAll(finder.documents());
        for (InternalLink link : finder.internalLinks())
        {
            var sourceDocument = link.sourceLocation().document().name();
            var targetDocument = link.targetDocument();
            outgoingLinks.computeIfAbsent(sourceDocument, key -> new ArrayList<>()).add(link);
            incomingLinks.computeIfAbsent(targetDocument, key -> new ArrayList<>()).add(link);
        }
    }

    private static class Finder
            extends InternalLinkFinder
    {
        private final Set<String> documents = new HashSet<>();

        @Override
        public void visit(Document document)
        {
            documents.add(document.name());
            super.visit(document);
        }

        public Set<String> documents()
        {
            return documents;
        }
    }
}

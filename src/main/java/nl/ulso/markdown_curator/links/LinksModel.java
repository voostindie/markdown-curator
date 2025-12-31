package nl.ulso.markdown_curator.links;

import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.DataModelTemplate;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.event.*;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.*;

import static java.util.Collections.emptyList;
import static nl.ulso.markdown_curator.Changelog.emptyChangelog;

@Singleton
public final class LinksModel
        extends DataModelTemplate
{
    private final Vault vault;
    private final Map<String, Document> documentIndex;

    @Inject
    public LinksModel(Vault vault)
    {
        this.vault = vault;
        this.documentIndex = new HashMap<>();
    }

    List<String> deadLinksFor(String documentName)
    {
        var source = documentIndex.get(documentName);
        if (source == null)
        {
            return emptyList();
        }
        return source.findInternalLinks().stream()
                .map(InternalLink::targetDocument)
                .filter(document -> !document.isBlank())
                .filter(document -> !documentIndex.containsKey(document))
                .sorted()
                .distinct()
                .toList();
    }

    @Override
    public Changelog fullRefresh(Changelog changelog)
    {
        documentIndex.clear();
        indexDocuments(vault);
        return emptyChangelog();
    }

    @Override
    public Changelog process(DocumentAdded event, Changelog changelog)
    {
        indexDocuments(event.document());
        return emptyChangelog();
    }

    @Override
    public Changelog process(DocumentChanged event, Changelog changelog)
    {
        indexDocuments(event.document());
        return emptyChangelog();
    }

    @Override
    public Changelog process(DocumentRemoved event, Changelog changelog)
    {
        var documentName = event.document().name();
        documentIndex.remove(documentName);
        return emptyChangelog();
    }

    @Override
    public Changelog process(FolderAdded event, Changelog changelog)
    {
        indexDocuments(event.folder());
        return emptyChangelog();
    }

    @Override
    public Changelog process(FolderRemoved event, Changelog changelog)
    {
        var finder = new DocumentFinder();
        event.folder().accept(finder);
        for (String document : finder.documents.keySet())
        {
            documentIndex.remove(document);
        }
        return emptyChangelog();
    }

    private void indexDocuments(Visitable visitable)
    {
        var finder = new DocumentFinder();
        visitable.accept(finder);
        documentIndex.putAll(finder.documents);
    }

    private static class DocumentFinder
            extends BreadthFirstVaultVisitor
    {
        private final Map<String, Document> documents = new HashMap<>();

        @Override
        public void visit(Document document)
        {
            documents.put(document.name(), document);
        }
    }
}

package nl.ulso.markdown_curator.links;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.DataModelTemplate;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.event.*;

import java.util.*;

import static java.util.Collections.emptyList;

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
    protected void fullRefresh()
    {
        documentIndex.clear();
        indexDocuments(vault);
    }

    @Override
    public void process(DocumentAdded event)
    {
        indexDocuments(event.document());
    }

    @Override
    public void process(DocumentChanged event)
    {
        indexDocuments(event.document());
    }

    @Override
    public void process(DocumentRemoved event)
    {
        var documentName = event.document().name();
        documentIndex.remove(documentName);
    }

    @Override
    public void process(FolderAdded event)
    {
        indexDocuments(event.folder());
    }

    @Override
    public void process(FolderRemoved event)
    {
        var finder = new DocumentFinder();
        event.folder().accept(finder);
        for (String document : finder.documents.keySet())
        {
            documentIndex.remove(document);
        }
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

package nl.ulso.markdown_curator.links;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.*;
import nl.ulso.markdown_curator.vault.*;

import java.util.*;

import static java.util.Collections.emptyList;
import static nl.ulso.markdown_curator.Change.Kind.DELETE;

@Singleton
public final class LinksModel
    extends ChangeProcessorTemplate
{
    private final Vault vault;
    private final Map<String, Document> documentIndex;

    @Inject
    public LinksModel(Vault vault)
    {
        this.vault = vault;
        this.documentIndex = new HashMap<>();
        this.registerChangeHandler(isObjectType(Document.class), this::processDocumentChange);
        this.registerChangeHandler(isObjectType(Folder.class), this::processFolderChange);
    }

    @Override
    public Collection<Change<?>> fullRefresh()
    {
        documentIndex.clear();
        indexDocuments(vault);
        return emptyList();
    }

    private Collection<Change<?>> processDocumentChange(Change<?> change)
    {
        var document = (Document) change.object();
        if (change.kind() == DELETE)
        {
            documentIndex.remove(document.name());
        }
        else
        {
            indexDocuments(document);
        }
        return emptyList();
    }

    private Collection<Change<?>> processFolderChange(Change<?> change)
    {
        var folder = (Folder) change.object();
        if (change.kind() == DELETE)
        {
            var finder = new DocumentFinder();
            folder.accept(finder);
            for (String document : finder.documents.keySet())
            {
                documentIndex.remove(document);
            }
        }
        else
        {
            indexDocuments(folder);
        }
        return emptyList();
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

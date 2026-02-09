package nl.ulso.curator.addon.links;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;
import nl.ulso.curator.vault.*;

import java.util.*;

import static java.util.Collections.emptyList;
import static nl.ulso.curator.change.Change.Kind.DELETE;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

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
    }

    @Override
    protected Set<? extends ChangeHandler> createChangeHandlers()
    {
        return Set.of(
            newChangeHandler(isPayloadType(Document.class), this::processDocumentChange),
            newChangeHandler(isPayloadType(Folder.class), this::processFolderChange)
        );
    }

    @Override
    public void reset(ChangeCollector collector)
    {
        documentIndex.clear();
        indexDocuments(vault);
    }

    private void processDocumentChange(Change<?> change, ChangeCollector collector)
    {
        var document = (Document) change.value();
        if (change.kind() == DELETE)
        {
            documentIndex.remove(document.name());
        }
        else
        {
            indexDocuments(document);
        }
    }

    private void processFolderChange(Change<?> change, ChangeCollector collector)
    {
        var folder = (Folder) change.value();
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

package nl.ulso.curator.addon.backlink;

import jakarta.inject.Inject;
import nl.ulso.curator.change.ChangeCollector;
import nl.ulso.curator.change.EntityProcessor;
import nl.ulso.curator.extension.ExtensionScope;
import nl.ulso.curator.vault.*;

import java.util.HashSet;
import java.util.Set;

import static nl.ulso.curator.addon.backlink.BacklinkQuery.BACKLINK_QUERY_NAME;
import static nl.ulso.curator.addon.backlink.BacklinkQuery.resolveDocumentName;

/// Produces [BacklinkQueryReference]s by finding occurrences of the `backlink` query in documents
/// and parsing their query configuration.
///
/// [BacklinkQueryReference]s are only created and deleted, never updated.
@ExtensionScope
final class BacklinkQueryReferenceProducer
    extends EntityProcessor<Document>
{
    @Inject
    BacklinkQueryReferenceProducer()
    {
    }

    @Override
    protected Class<Document> entityClass()
    {
        return Document.class;
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(BacklinkQueryReference.class);
    }

    @Override
    protected void entityCreated(Document newDocument, ChangeCollector collector)
    {
        findBacklinkQueryReferences(newDocument).forEach(
            reference -> collector.create(reference, BacklinkQueryReference.class)
        );
    }

    @Override
    protected void entityUpdated(
        Document oldDocument, Document newDocument, ChangeCollector collector)
    {
        var oldReferences = findBacklinkQueryReferences(oldDocument);
        var newReferences = findBacklinkQueryReferences(newDocument);
        if (!oldReferences.isEmpty())
        {
            var deletedReferences = new HashSet<>(oldReferences);
            deletedReferences.removeAll(newReferences);
            deletedReferences.forEach(
                reference -> collector.delete(reference, BacklinkQueryReference.class)
            );
        }
        if (!newReferences.isEmpty())
        {
            var createdReferences = new HashSet<>(newReferences);
            createdReferences.removeAll(oldReferences);
            createdReferences.forEach(
                reference -> collector.create(reference, BacklinkQueryReference.class)
            );
        }
    }

    @Override
    protected void entityDeleted(Document oldDocument, ChangeCollector collector)
    {
        findBacklinkQueryReferences(oldDocument).forEach(
            backlinkQueryReference -> collector.delete(backlinkQueryReference,
                BacklinkQueryReference.class
            ));
    }

    private Set<BacklinkQueryReference> findBacklinkQueryReferences(Document document)
    {
        var finder = new BacklinkQueryFinder();
        document.accept(finder);
        return finder.references;
    }

    private static final class BacklinkQueryFinder
        extends BreadthFirstVaultVisitor
    {
        private final Set<BacklinkQueryReference> references = new HashSet<>();

        @Override
        public void visit(QueryBlock queryBlock)
        {
            if (queryBlock.queryName().contentEquals(BACKLINK_QUERY_NAME))
            {
                references.add(new BacklinkQueryReference(
                    queryBlock.document().name(), resolveDocumentName(queryBlock)
                ));
            }
        }
    }
}

package nl.ulso.curator.addon.backlink;

import jakarta.inject.Inject;
import nl.ulso.curator.change.ChangeCollector;
import nl.ulso.curator.change.EntityProcessor;
import nl.ulso.curator.extension.ExtensionScope;
import nl.ulso.curator.vault.*;

import java.util.Set;

/// Produces backlinks for all documents in the vault whenever a query reference to a previously
/// untracked document is added.
///
/// This processor is executed *before* the [BacklinkQueryReferenceRepository] is updated. That
/// means that when a new [BacklinkQueryReference] comes in and the repository has no references to
/// the backlink document name yet, it concerns a completely new reference. In that case all links
/// to the document are looked up across the vault, and [Backlink]s are created for every instance,
/// at most one per document.
///
/// This producer claims it produces a [BacklinkInitializer] change, while it actually does not. The
/// [BacklinkQueryReferenceRepository] claims it requires it, however. This little trick ensures the
/// producer comes before the repository in the execution pipeline.
@ExtensionScope
final class BacklinkInitializer
    extends EntityProcessor<BacklinkQueryReference>
{
    private final BacklinkQueryReferenceRepository repository;
    private final Vault vault;

    @Inject
    BacklinkInitializer(BacklinkQueryReferenceRepository repository, Vault vault)
    {
        this.repository = repository;
        this.vault = vault;
    }

    @Override
    protected Class<BacklinkQueryReference> entityClass()
    {
        return BacklinkQueryReference.class;
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(Backlink.class, BacklinkInitializer.class);
    }

    @Override
    protected void entityCreated(BacklinkQueryReference reference, ChangeCollector collector)
    {
        var backlinkDocumentName = reference.backlinkDocumentName();
        if (!repository.hasBacklinkReference(backlinkDocumentName))
        {
            vault.accept(new BacklinkFinder(backlinkDocumentName, collector));
        }
    }

    private static class BacklinkFinder
        extends BreadthFirstVaultVisitor
    {
        private final String backlinkDocumentName;
        private final ChangeCollector collector;

        private BacklinkFinder(String backlinkDocumentName, ChangeCollector collector)
        {
            this.backlinkDocumentName = backlinkDocumentName;
            this.collector = collector;
        }

        @Override
        public void visit(Document document)
        {
            if (document.findInternalLinks().stream()
                .map(InternalLink::targetDocument)
                .anyMatch(targetDocumentName -> targetDocumentName.equals(backlinkDocumentName)))
            {
                collector.create(
                    new Backlink(backlinkDocumentName, document.name()),
                    Backlink.class
                );
            }
        }
    }
}

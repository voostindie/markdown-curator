package nl.ulso.curator.addon.backlink;

import jakarta.inject.Inject;
import nl.ulso.curator.change.ChangeCollector;
import nl.ulso.curator.change.EntityProcessor;
import nl.ulso.curator.extension.ExtensionScope;

import java.util.Set;

/// Deletes all backlinks for a document when the last reference to it is deleted.
///
/// This processor comes after the [BacklinkQueryReferenceRepository]. That means it can check
/// whether the backlink document in a [BacklinkQueryReference] is no longer known by the
/// repository. If not, then all links to that document known by the repository are deleted.
@ExtensionScope
final class BacklinkExpunger
    extends EntityProcessor<BacklinkQueryReference>
{
    private final BacklinkQueryReferenceRepository backlinkQueryReferenceRepository;
    private final BacklinkRepository backlinkRepository;

    @Inject
    BacklinkExpunger(
        BacklinkQueryReferenceRepository backlinkQueryReferenceRepository,
        BacklinkRepository backlinkRepository)
    {
        this.backlinkQueryReferenceRepository = backlinkQueryReferenceRepository;
        this.backlinkRepository = backlinkRepository;
    }

    @Override
    protected Class<BacklinkQueryReference> entityClass()
    {
        return BacklinkQueryReference.class;
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(Backlink.class);
    }

    @Override
    public Set<Class<?>> requiredPayloadTypes()
    {
        return Set.of(BacklinkQueryReferenceRepository.class);
    }

    @Override
    protected void entityDeleted(BacklinkQueryReference reference, ChangeCollector collector)
    {
        var backlinkDocumentName = reference.backlinkDocumentName();
        if (!backlinkQueryReferenceRepository.hasBacklinkReference(backlinkDocumentName))
        {
            backlinkRepository.backlinksFor(backlinkDocumentName).forEach(link ->
                collector.delete(new Backlink(backlinkDocumentName, link), Backlink.class));
        }
    }
}

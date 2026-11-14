package nl.ulso.curator.addon.backlink;

import jakarta.inject.Inject;
import nl.ulso.curator.change.SetBasedEntityRepository;
import nl.ulso.curator.extension.ExtensionScope;

import java.util.Set;
import java.util.stream.Collectors;

@ExtensionScope
final class DefaultBacklinkQueryReferenceRepository
    extends SetBasedEntityRepository<BacklinkQueryReference>
    implements BacklinkQueryReferenceRepository
{
    @Inject
    DefaultBacklinkQueryReferenceRepository()
    {
    }

    @Override
    protected Class<?> repositoryClass()
    {
        return BacklinkQueryReferenceRepository.class;
    }

    @Override
    protected Class<BacklinkQueryReference> entityClass()
    {
        return BacklinkQueryReference.class;
    }

    @Override
    public Set<Class<?>> requiredPayloadTypes()
    {
        return Set.of(BacklinkInitializer.class);
    }

    @Override
    public boolean hasBacklinkReference(String backlinkDocumentName)
    {
        return set().stream().anyMatch(reference ->
            reference.backlinkDocumentName().contentEquals(backlinkDocumentName)
        );
    }

    @Override
    public Set<String> backlinkDocumentNames()
    {
        return set().stream()
            .map(BacklinkQueryReference::backlinkDocumentName)
            .collect(Collectors.toSet());
    }
}

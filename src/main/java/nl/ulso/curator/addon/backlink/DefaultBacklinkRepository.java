package nl.ulso.curator.addon.backlink;

import jakarta.inject.Inject;
import nl.ulso.curator.change.SetBasedEntityRepository;
import nl.ulso.curator.extension.ExtensionScope;
import nl.ulso.curator.statistics.MeasurementTracker;

import java.util.Set;
import java.util.stream.Collectors;

@ExtensionScope
final class DefaultBacklinkRepository
    extends SetBasedEntityRepository<Backlink>
    implements MeasurementTracker, BacklinkRepository
{
    @Inject
    DefaultBacklinkRepository()
    {
    }

    @Override
    protected Class<Backlink> entityClass()
    {
        return Backlink.class;
    }

    @Override
    protected Class<?> repositoryClass()
    {
        return BacklinkRepository.class;
    }

    @Override
    public Set<String> backlinksFor(String documentName)
    {
        return set().stream()
            .filter(backlink -> backlink.targetDocumentName().contentEquals(documentName))
            .map(Backlink::sourceDocumentName)
            .collect(Collectors.toSet());
    }
}

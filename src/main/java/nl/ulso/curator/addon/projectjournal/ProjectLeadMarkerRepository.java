package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import nl.ulso.curator.extension.ExtensionScope;

/// Repository for [ProjectLeadMarker]s.
@ExtensionScope
final class ProjectLeadMarkerRepository
    extends ProjectMarkerRepository<ProjectLeadMarker>
{
    @Inject
    ProjectLeadMarkerRepository()
    {
    }

    @Override
    protected Class<?> repositoryClass()
    {
        return ProjectLeadMarkerRepository.class;
    }

    @Override
    protected Class<ProjectLeadMarker> entityClass()
    {
        return ProjectLeadMarker.class;
    }
}

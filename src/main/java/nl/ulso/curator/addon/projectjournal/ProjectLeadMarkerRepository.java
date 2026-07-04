package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/// Repository for [ProjectLeadMarker]s.
@Singleton
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

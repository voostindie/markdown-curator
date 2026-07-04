package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/// Repository for [ProjectStatusMarker]s.
@Singleton
final class ProjectStatusMarkerRepository
    extends ProjectMarkerRepository<ProjectStatusMarker>
{
    @Inject
    ProjectStatusMarkerRepository()
    {
    }

    @Override
    protected Class<?> repositoryClass()
    {
        return ProjectStatusMarkerRepository.class;
    }

    @Override
    protected Class<ProjectStatusMarker> entityClass()
    {
        return ProjectStatusMarker.class;
    }
}

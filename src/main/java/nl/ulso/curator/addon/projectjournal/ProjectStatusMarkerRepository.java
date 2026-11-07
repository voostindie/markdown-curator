package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import nl.ulso.curator.extension.ExtensionScope;

/// Repository for [ProjectStatusMarker]s.
@ExtensionScope
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

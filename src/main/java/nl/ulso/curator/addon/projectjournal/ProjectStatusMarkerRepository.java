package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.journal.Marker;

import static nl.ulso.curator.addon.projectjournal.ProjectStatusMarker.isProjectStatusMarker;

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
    protected Class<ProjectStatusMarker> targetEntityClass()
    {
        return ProjectStatusMarker.class;
    }

    @Override
    protected boolean isProjectMarker(Marker marker)
    {
        return isProjectStatusMarker(marker);
    }

    @Override
    protected ProjectStatusMarker createProjectMarker(Marker marker)
    {
        return new ProjectStatusMarker(marker);
    }
}

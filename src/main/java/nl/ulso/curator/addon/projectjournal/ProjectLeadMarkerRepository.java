package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.journal.Marker;

import static nl.ulso.curator.addon.projectjournal.ProjectLeadMarker.isProjectLeadMarker;

/// Repository for [ProjectStatusMarker]s.
@Singleton
final class ProjectLeadMarkerRepository
    extends ProjectMarkerRepository<ProjectLeadMarker>
{
    @Inject
    ProjectLeadMarkerRepository()
    {
    }

    @Override
    protected Class<ProjectLeadMarker> targetEntityClass()
    {
        return ProjectLeadMarker.class;
    }

    @Override
    protected boolean isProjectMarker(Marker marker)
    {
        return isProjectLeadMarker(marker);
    }

    @Override
    protected ProjectLeadMarker createProjectMarker(Marker marker)
    {
        return new ProjectLeadMarker(marker);
    }
}

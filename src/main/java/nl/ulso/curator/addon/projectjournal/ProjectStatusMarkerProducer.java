package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.journal.Marker;

import static nl.ulso.curator.addon.projectjournal.ProjectStatusMarker.isProjectStatusMarker;

@Singleton
final class ProjectStatusMarkerProducer
    extends ProjectMarkerProducer<ProjectStatusMarker>
{
    @Inject
    ProjectStatusMarkerProducer()
    {
    }

    @Override
    protected Class<ProjectStatusMarker> targetClass()
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

package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.journal.Marker;

import static nl.ulso.curator.addon.projectjournal.ProjectLeadMarker.isProjectLeadMarker;

@Singleton
final class ProjectLeadMarkerProducer
    extends ProjectMarkerProducer<ProjectLeadMarker>
{
    @Inject
    ProjectLeadMarkerProducer()
    {
    }

    @Override
    protected Class<ProjectLeadMarker> targetClass()
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

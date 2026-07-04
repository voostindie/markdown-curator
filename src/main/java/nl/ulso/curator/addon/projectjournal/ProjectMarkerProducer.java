package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.addon.journal.Marker;
import nl.ulso.curator.change.EntityTransformer;

import java.util.Optional;

/// Base class for change processors that produce project markers from plain markers.
abstract class ProjectMarkerProducer<P extends ProjectMarker>
    extends EntityTransformer<Marker, P>
{
    protected abstract boolean isProjectMarker(Marker marker);

    protected abstract P createProjectMarker(Marker marker);

    @Override
    protected Class<Marker> sourceClass()
    {
        return Marker.class;
    }

    @Override
    protected final Optional<P> transform(Marker marker)
    {
        if (isProjectMarker(marker))
        {
            return Optional.of(createProjectMarker(marker));
        }
        return Optional.empty();
    }
}

package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.addon.journal.Marker;
import nl.ulso.curator.change.MapBasedEntityRepository;

import java.util.*;

/// Base class for repositories of project markers. The repository keeps track of a specific
/// [ProjectMarker] subclass, not all. In other words: every [ProjectMarker] subclass needs it
/// own repository.
///
/// In theory a single [ProjectMarker] can have multiple matching marker documents, but in practice
/// there's just one, or zero.
abstract class ProjectMarkerRepository<P extends ProjectMarker>
    extends MapBasedEntityRepository<Marker, String, P>
{
    @Override
    protected final Class<Marker> sourceEntityClass()
    {
        return Marker.class;
    }

    @Override
    protected final boolean isEntity(Marker marker)
    {
        return isProjectMarker(marker);
    }

    @Override
    protected final String entityKeyFrom(Marker marker)
    {
        return marker.name();
    }

    @Override
    protected final P createEntityFrom(String documentName, Marker marker)
    {
        return createProjectMarker(marker);
    }

    @Override
    protected final Map<String, P> createMap()
    {
        return new HashMap<>(1);
    }

    protected abstract boolean isProjectMarker(Marker marker);

    protected abstract P createProjectMarker(Marker marker);

    final Set<String> allMarkers()
    {
        return map().keySet();
    }

    final P markerNamed(String name)
    {
        return map().get(name);
    }
}

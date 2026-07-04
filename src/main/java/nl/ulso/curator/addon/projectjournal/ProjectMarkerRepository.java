package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.change.MapBasedEntityRepository;

import java.util.Map;
import java.util.Set;

import static java.util.HashMap.newHashMap;

/// Base class for repositories of project markers. The repository keeps track of a specific
/// [ProjectMarker] subclass, not all. In other words: every [ProjectMarker] subclass needs its own
/// repository.
///
/// In theory a single [ProjectMarker] can have multiple matching marker documents, but in practice
/// there's just one, or zero.
abstract class ProjectMarkerRepository<P extends ProjectMarker>
    extends MapBasedEntityRepository<String, P>
{
    @Override
    protected final String entityKeyFrom(P marker)
    {
        return marker.name();
    }

    @Override
    protected final Map<String, P> createMap()
    {
        return newHashMap(1);
    }

    final Set<String> allMarkers()
    {
        return map().keySet();
    }

    final P markerNamed(String name)
    {
        return map().get(name);
    }
}

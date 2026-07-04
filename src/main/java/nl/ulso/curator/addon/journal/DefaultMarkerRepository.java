package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.MapBasedEntityRepository;
import nl.ulso.dictionary.Dictionary;

import java.util.Map;

import static nl.ulso.dictionary.Dictionary.emptyDictionary;

@Singleton
final class DefaultMarkerRepository
    extends MapBasedEntityRepository<String, Marker>
    implements MarkerRepository
{
    @Inject
    DefaultMarkerRepository()
    {
    }

    @Override
    protected Class<Marker> entityClass()
    {
        return Marker.class;
    }

    @Override
    protected Class<?> repositoryClass()
    {
        return MarkerRepository.class;
    }

    @Override
    protected String entityKeyFrom(Marker marker)
    {
        return marker.name();
    }

    @Override
    public Map<String, Marker> markers()
    {
        return map();
    }

    @Override
    public Dictionary markerSettings(String markerName)
    {
        var marker = map().get(markerName);
        return marker != null ? marker.settings() : emptyDictionary();
    }
}

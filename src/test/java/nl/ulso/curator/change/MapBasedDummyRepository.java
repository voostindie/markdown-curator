package nl.ulso.curator.change;

import java.util.Map;

/// Simple map-based repository of [Dummy] objects, for testing purposes.
final class MapBasedDummyRepository
    extends MapBasedEntityRepository<String, Dummy>
{
    private Map<String, Dummy> mutableMap;

    MapBasedDummyRepository()
    {
        this(null);
    }

    MapBasedDummyRepository(Dummy initialState)
    {
        if (initialState != null)
        {
            // mutableMap won't be null because the superclass constructor initializes it.
            mutableMap.put(initialState.name(), initialState);
        }
    }

    @Override
    protected Class<Dummy> entityClass()
    {
        return Dummy.class;
    }

    @Override
    protected Class<?> repositoryClass()
    {
        return MapBasedDummyRepository.class;
    }

    @Override
    protected Map<String, Dummy> createMap()
    {
        var map = super.createMap();
        this.mutableMap = map;
        return map;
    }

    @Override
    protected String entityKeyFrom(Dummy dummy)
    {
        return dummy.name();
    }
}

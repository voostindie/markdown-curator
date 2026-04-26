package nl.ulso.curator.change;

import nl.ulso.curator.vault.Document;

import java.util.Map;

/// Simple map-based repository of [Dummy] objects, for testing purposes.
///
/// Every document that has a property `dummy` is considered a [Dummy] candidate.
final class MapBasedDummyRepository
    extends MapBasedEntityRepository<Document, String, Dummy>
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
            // mutableMap won't be null, because the superclass constructor initializes it.
            mutableMap.put(initialState.name(), initialState);
        }
    }

    @Override
    protected Map<String, Dummy> createMap()
    {
        var map = super.createMap();
        this.mutableMap = map;
        return map;
    }

    @Override
    protected Class<Document> sourceEntityClass()
    {
        return Document.class;
    }

    @Override
    protected Class<Dummy> targetEntityClass()
    {
        return Dummy.class;
    }

    @Override
    protected boolean isEntity(Document document)
    {
        return document.frontMatter().hasProperty("dummy");
    }

    @Override
    protected String entityKeyFrom(Document document)
    {
        return document.name();
    }

    @Override
    protected Dummy createEntityFrom(String name, Document document)
    {
        return new Dummy(name);
    }
}

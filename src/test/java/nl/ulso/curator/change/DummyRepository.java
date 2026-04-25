package nl.ulso.curator.change;

import nl.ulso.curator.vault.Document;

import java.util.Map;

/// Simple repository of [Dummy] objects, for testing purposes.
///
/// Every document that has a property `dummy` is considered a [Dummy] candidate.
final class DummyRepository
    extends DocumentBasedEntityRepository<String, Dummy>
{
    private Map<String, Dummy> mutableMap;

    DummyRepository()
    {
        this(null);
    }

    DummyRepository(Dummy initialState)
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
    protected Class<Dummy> entityClass()
    {
        return Dummy.class;
    }

    @Override
    protected boolean isEntity(Document document)
    {
        return document.frontMatter().hasProperty("dummy");
    }

    @Override
    protected Dummy createEntityFrom(Document document)
    {
        return new Dummy(document.name());
    }

    @Override
    protected String entityKeyFrom(Document document)
    {
        return document.name();
    }
}

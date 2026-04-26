package nl.ulso.curator.change;

import nl.ulso.curator.vault.Document;

import java.util.Set;

/// Simple set-based repository of [Dummy] objects, for testing purposes.
///
/// Every document that has a property `dummy` is considered a [Dummy] candidate.
final class SetBasedDummyRepository
    extends SetBasedEntityRepository<Document, Dummy>
{
    private Set<Dummy> mutableSet;

    SetBasedDummyRepository()
    {
        this(null);
    }

    SetBasedDummyRepository(Dummy initialState)
    {
        if (initialState != null)
        {
            // mutableSet won't be null, because the superclass constructor initializes it.
            mutableSet.add(initialState);
        }
    }

    @Override
    protected Set<Dummy> createSet()
    {
        var set = super.createSet();
        this.mutableSet = set;
        return set;
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
    protected Dummy createEntityFrom(Document document)
    {
        return new Dummy(document.name());
    }
}

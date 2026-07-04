package nl.ulso.curator.change;

import java.util.Set;

/// Simple set-based repository of [Dummy] objects, for testing purposes.
///
/// Every document that has a property `dummy` is considered a [Dummy] candidate.
final class SetBasedDummyRepository
    extends SetBasedEntityRepository<Dummy>
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
            // mutableSet won't be null because the superclass constructor initializes it.
            mutableSet.add(initialState);
        }
    }

    @Override
    protected Class<?> repositoryClass()
    {
        return SetBasedDummyRepository.class;
    }

    @Override
    protected Class<Dummy> entityClass()
    {
        return Dummy.class;
    }

    @Override
    protected Set<Dummy> createSet()
    {
        var set = super.createSet();
        this.mutableSet = set;
        return set;
    }
}

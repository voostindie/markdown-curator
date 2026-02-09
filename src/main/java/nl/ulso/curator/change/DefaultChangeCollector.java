package nl.ulso.curator.change;

import java.util.Collection;

final class DefaultChangeCollector
    implements ChangeCollector
{
    private final Collection<Change<?>> collection;

    DefaultChangeCollector(Collection<Change<?>> collection)
    {
        this.collection = collection;
    }

    @Override
    public <T> void add(Change<T> change)
    {
        collection.add(change);
    }

    @Override
    public <T> void create(T newValue, Class<T> payloadType)
    {
        add(Change.create(newValue, payloadType));
    }

    @Override
    public <T> void update(T value, Class<T> payloadType)
    {
        add(Change.update(value, payloadType));
    }

    @Override
    public <T> void update(T oldValue, T newValue, Class<T> payloadType)
    {
        add(Change.update(oldValue, newValue, payloadType));
    }

    @Override
    public <T> void delete(T oldValue, Class<T> payloadType)
    {
        add(Change.delete(oldValue, payloadType));
    }

    @Override
    public Changelog changelog()
    {
        return Changelog.changelogFor(collection);
    }
}

package nl.ulso.curator.change;

import java.util.stream.Stream;

import static nl.ulso.curator.change.Change.Kind.UPDATE;

record Update2<T>(T oldValue, T newValue, Class<T> payloadType)
    implements Change<T>
{
    @Override
    public T value()
    {
        return newValue;
    }

    @Override
    public Stream<T> values()
    {
        return Stream.of(oldValue, newValue);
    }

    @Override
    public Kind kind()
    {
        return UPDATE;
    }
}

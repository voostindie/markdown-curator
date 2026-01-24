package nl.ulso.markdown_curator;

import static nl.ulso.markdown_curator.Change.Kind.UPDATE;

record Update2<T>(T oldValue, T newValue, Class<T> payloadType)
    implements Change<T>
{
    @Override
    public T value()
    {
        return newValue;
    }

    @Override
    public Kind kind()
    {
        return UPDATE;
    }
}

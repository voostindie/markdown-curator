package nl.ulso.markdown_curator;

import static nl.ulso.markdown_curator.Change.Kind.UPDATE;

record Update2<T>(T oldObject, T newObject, Class<T> objectType)
    implements Change<T>
{
    @Override
    public T object()
    {
        return newObject;
    }

    @Override
    public Kind kind()
    {
        return UPDATE;
    }
}

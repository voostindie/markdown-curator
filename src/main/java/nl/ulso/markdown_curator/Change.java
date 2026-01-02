package nl.ulso.markdown_curator;

public interface Change<T>
{
    enum Kind
    {
        CREATE,
        UPDATE,
        DELETE
    }

    static <T> Change<T> create(T object, Class<T> objectType)
    {
        return new ChangeImpl<>(object, objectType, Kind.CREATE);
    }

    static <T> Change<T> update(T object, Class<T> objectType)
    {
        return new ChangeImpl<>(object, objectType, Kind.UPDATE);
    }

    static <T> Change<T> delete(T object, Class<T> objectType)
    {
        return new ChangeImpl<>(object, objectType, Kind.DELETE);
    }

    T object();

    <U> Change<U> as(Class<U> objectType);

    <U> U objectAs(Class<U> objectType);

    Class<?> objectType();

    Kind kind();
}

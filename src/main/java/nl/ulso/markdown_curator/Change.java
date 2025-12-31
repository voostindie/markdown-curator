package nl.ulso.markdown_curator;

public interface Change<T>
{
    enum Kind
    {
        CREATION,
        MODIFICATION,
        DELETION
    }

    static <T> Change<T> creation(T object, Class<T> objectType)
    {
        return new ChangeImpl<>(object, objectType, Kind.CREATION);
    }

    static <T> Change<T> modification(T object, Class<T> objectType)
    {
        return new ChangeImpl<>(object, objectType, Kind.MODIFICATION);
    }

    static <T> Change<T> deletion(T object, Class<T> objectType)
    {
        return new ChangeImpl<>(object, objectType, Kind.DELETION);
    }

    T object();

    Class<?> objectType();

    Kind kind();
}

package nl.ulso.markdown_curator;

import java.util.function.Predicate;

import static nl.ulso.markdown_curator.Change.Kind.CREATE;
import static nl.ulso.markdown_curator.Change.Kind.DELETE;
import static nl.ulso.markdown_curator.Change.Kind.UPDATE;

public sealed interface Change<T>
    permits Create, Update1, Update2, Delete
{
    enum Kind
    {
        CREATE,
        UPDATE,
        DELETE
    }

    static <T> Change<T> create(T newObject, Class<T> objectType)
    {
        return new Create<>(newObject, objectType);
    }

    static <T> Change<T> update(T object, Class<T> objectType)
    {
        return new Update1<>(object, objectType);
    }

    static <T> Change<T> update(T oldObject, T newObject, Class<T> objectType)
    {
        return new Update2<>(oldObject, newObject, objectType);
    }

    static <T> Change<T> delete(T oldObject, Class<T> objectType)
    {
        return new Delete<>(oldObject, objectType);
    }

    static Predicate<Change<?>> isObjectType(Class<?> objectType)
    {
        return change -> change.objectType().equals(objectType);
    }

    static Predicate<Change<?>> isCreate()
    {
        return change -> change.kind() == CREATE;
    }

    static Predicate<Change<?>> isUpdate()
    {
        return change -> change.kind() == UPDATE;
    }

    static Predicate<Change<?>> isCreateOrUpdate()
    {
        return isCreate().or(isUpdate());
    }

    static Predicate<Change<?>> isDelete()
    {
        return change -> change.kind() == DELETE;
    }

    T object();

    T oldObject();

    T newObject();

    Class<?> objectType();

    Kind kind();

    @SuppressWarnings("unchecked")
    default <U> Change<U> as(Class<U> objectType)
    {
        if (!this.objectType().equals(objectType))
        {
            throw new IllegalStateException("Cannot cast change to different object type");
        }
        return (Change<U>) this;
    }
}
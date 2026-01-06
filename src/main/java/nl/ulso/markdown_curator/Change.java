package nl.ulso.markdown_curator;

import java.util.function.Predicate;

import static nl.ulso.markdown_curator.Change.Kind.CREATE;
import static nl.ulso.markdown_curator.Change.Kind.DELETE;
import static nl.ulso.markdown_curator.Change.Kind.UPDATE;

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

    <U> Change<U> as(Class<U> objectType);

    <U> U objectAs(Class<U> objectType);

    Class<?> objectType();

    Kind kind();
}

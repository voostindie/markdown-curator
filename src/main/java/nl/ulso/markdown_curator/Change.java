package nl.ulso.markdown_curator;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static nl.ulso.markdown_curator.Change.Kind.CREATE;
import static nl.ulso.markdown_curator.Change.Kind.DELETE;
import static nl.ulso.markdown_curator.Change.Kind.UPDATE;

/// Represents a change in the [Changelog]; a change has a payload: a type ([Class]) and one or two
/// values of that type, for inspection.
///
/// There are 3 different kinds, of in total 4 types of changes:
///
/// 1. `CREATE`: a new object is created. This new object is in the payload of the change. Both in
/// [#value()] and in [#newValue()].
/// 2. `UPDATE` single: a single object is updated, but the actual change is not clear. The changed
/// object is in the payload of the change, both in [#value()] and in [#newValue()].
/// 3. `UPDATE` double: an object is replaced by a different object. The old and new objects are
/// both in the payload of the change, in [#oldValue()] and [#newValue()]. [#value()] returns the
/// new value.
/// 4. `DELETE`: an object is deleted. The deleted object is in the payload of the change, both in
/// [#value()] and in [#oldValue()].
///
/// Changes are often used in [Predicate]s. To aid the creation of such predicates, this class
/// contains several methods to help build them.
public sealed interface Change<T>
    permits Create, Update1, Update2, Delete
{
    enum Kind
    {
        CREATE,
        UPDATE,
        DELETE
    }

    /// @param newValue    the new object that was created.
    /// @param payloadType the type of the object that was created.
    /// @return a new `CREATE` change.
    static <T> Change<T> create(T newValue, Class<T> payloadType)
    {
        return new Create<>(newValue, payloadType);
    }

    /// @param value       the value that is updated.
    /// @param payloadType the type of the value that was updated.
    /// @return a new `UPDATE` change.
    static <T> Change<T> update(T value, Class<T> payloadType)
    {
        return new Update1<>(value, payloadType);
    }

    /// @param oldValue    the object that was replaced.
    /// @param newValue    the object that replaces the old object.
    /// @param payloadType the type of the object that was updated.
    /// @return a new `UPDATE` change.
    static <T> Change<T> update(T oldValue, T newValue, Class<T> payloadType)
    {
        return new Update2<>(oldValue, newValue, payloadType);
    }

    /// @param oldValue    the object that was deleted.
    /// @param payloadType the type of the object that was deleted.
    /// @return a new `DELETE` change.
    static <T> Change<T> delete(T oldValue, Class<T> payloadType)
    {
        return new Delete<>(oldValue, payloadType);
    }

    static Predicate<Change<?>> isPayloadType(Class<?> payloadType)
    {
        return change -> change.payloadType().equals(payloadType);
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

    /// @return the old value in the change.
    /// @throws UnsupportedOperationException if the change is a [Create] or [Update1].
    T oldValue();

    /// @return the new value in the change.
    /// @throws UnsupportedOperationException if the change is a [Delete].
    T newValue();

    /// Shorthand method to quickly get _a_ value from a change; the most relevant one, depending on
    /// the change's kind:
    ///
    /// - `CREATE`: the new value
    /// - `UPDATE`: the new value
    /// - `DELETE`: the old value
    ///
    /// @return the most relevant value of the change, either [#newValue()] or [#oldValue()].
    T value();

    /// Shorthand method to quickly get all values from a change - either one or two - depending on
    /// the change's kind and type.
    ///
    /// @return all values from the change.
    Stream<T> values();

    /// @return the type of the payload of the change.
    Class<?> payloadType();

    /// @return the kind of the change.
    Kind kind();

    /// Convenience method to cast a change to its payload type safely.
    @SuppressWarnings("unchecked")
    default <U> Change<U> as(Class<U> payloadType)
    {
        if (!this.payloadType().equals(payloadType))
        {
            throw new IllegalArgumentException("Cannot cast change to different payload type. " +
                                               "Actual: " + this.payloadType().getSimpleName() +
                                               ". Requested: " + payloadType.getSimpleName());
        }
        return (Change<U>) this;
    }
}
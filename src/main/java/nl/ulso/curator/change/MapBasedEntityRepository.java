package nl.ulso.curator.change;

import nl.ulso.curator.statistics.MeasurementCollector;
import nl.ulso.curator.statistics.MeasurementTracker;
import nl.ulso.curator.vault.Vault;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.*;
import static nl.ulso.curator.change.Change.*;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

/// Base class for repositories that transform source entities into target entities and store the
/// target entities in a map.
///
/// The repository is a [ChangeProcessor] that subscribes to changes of type [Vault] and the source
/// entity type. In case of a [Vault] event, the internal map is flushed. When it is a source entity
/// event, the source entity is mapped to a target entity if possible, the internal map is updated
/// accordingly, and a new event for the target entity type is published.
///
/// The internal map is mutated by the repository itself only. The API exposes only read-only views
/// it. This guarantees that the state is consistent.
///
/// Statistics about the target entity are gathered automatically.
///
/// In a Dagger module, the subclass needs to be a singleton and registered twice: once as a
/// [ChangeProcessor], and once as a [MeasurementTracker]. In both cases the instances must be bound
/// into a set.
public abstract class MapBasedEntityRepository<S, K, T>
    extends ChangeProcessorTemplate
    implements MeasurementTracker
{
    private final Map<K, T> map = createMap();

    @Override
    public final Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Vault.class, sourceEntityClass());
    }

    @Override
    public final Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(targetEntityClass());
    }

    @Override
    protected final List<? extends ChangeHandler> createChangeHandlers()
    {
        return List.of(
            newChangeHandler(isCreate().and(entityPredicate()), this::createEntity),
            newChangeHandler(isDelete().and(entityPredicate()), this::deleteEntity),
            newChangeHandler(isUpdate().and(entityPredicate()), this::updateEntity)
        );
    }

    private Predicate<? super Change<?>> entityPredicate()
    {
        return isPayloadType(sourceEntityClass()).and(
            change -> change.as(sourceEntityClass()).values().anyMatch(this::isEntity));
    }

    @Override
    protected final void reset()
    {
        map.clear();
    }

    private void createEntity(Change<?> change, ChangeCollector collector)
    {
        var sourceEntity = change.as(sourceEntityClass()).newValue();
        var entityKey = entityKeyFrom(sourceEntity);
        var targetEntity = createEntityFrom(entityKey, sourceEntity);
        map.put(entityKey, targetEntity);
        collector.add(create(targetEntity, targetEntityClass()));
    }

    private void deleteEntity(Change<?> change, ChangeCollector collector)
    {
        var entity = map.remove(entityKeyFrom(change.as(sourceEntityClass()).oldValue()));
        collector.add(delete(entity, targetEntityClass()));
    }

    private void updateEntity(Change<?> change, ChangeCollector collector)
    {
        var newSourceEntity = change.as(sourceEntityClass()).newValue();
        var entityKey = entityKeyFrom(newSourceEntity);
        if (isEntity(newSourceEntity))
        {
            // The new source entity represents a target entity...
            if (map.containsKey(entityKey))
            {
                // ...and so did the old source entity. So, we UPDATE the target entity.
                var newTargetEntity = createEntityFrom(entityKey, newSourceEntity);
                var oldTargetEntity = map.put(entityKey, newTargetEntity);
                collector.add(update(oldTargetEntity, newTargetEntity, targetEntityClass()));
            }
            else
            {
                // ...but the old source entity did not. So, we CREATE a new target entity.
                createEntity(change, collector);
            }
        }
        else
        {
            // The new source entity is not a target entity, but the old source entity was.
            // So, we DELETE the old target entity.
            deleteEntity(change, collector);
        }
    }

    @Override
    public void collectMeasurements(MeasurementCollector collector)
    {
        collector.total(targetEntityClass(), map.size());
    }

    /// @return a read-only view of the internal map.
    protected final Map<K, T> map()
    {
        return unmodifiableMap(map);
    }

    /// @return a read-only view of the internal map as a [NavigableMap].
    /// @throws ClassCastException if the implementation returned by [#createMap()] is not a
    /// [NavigableMap].
    protected final NavigableMap<K, T> navigableMap()
    {
        return unmodifiableNavigableMap((NavigableMap<K, T>) map);
    }

    /// @return a read-only view of the internal map as a [SortedMap].
    /// @throws ClassCastException if the implementation returned by [#createMap()] is not a
    /// [SortedMap].
    protected final SortedMap<K, T> sortedMap()
    {
        return unmodifiableSortedMap((SortedMap<K, T>) map);
    }

    /// @return a read-only view of the internal map as a [SequencedMap].
    /// @throws ClassCastException if the implementation returned by [#createMap()] is not a
    /// [SequencedMap].
    protected final SequencedMap<K, T> sequencedMap()
    {
        return unmodifiableSequencedMap((SequencedMap<K, T>) map);
    }

    /// @return the entity with the given key, if it exists.
    protected final Optional<T> findByKey(K key)
    {
        return Optional.ofNullable(map.get(key));
    }

    /// @return all entities in the repository.
    protected final Collection<T> entities()
    {
        return unmodifiableCollection(map.values());
    }

    /// @return the map to store entities in; by default, this is a [HashMap].
    protected Map<K, T> createMap()
    {
        return new HashMap<>();
    }

    protected abstract Class<S> sourceEntityClass();

    protected abstract Class<T> targetEntityClass();

    /// @param source the source entity to check.
    /// @return whether the source entity represents a target entity.
    protected abstract boolean isEntity(S source);

    /// @param source the source entity to create the key from.
    /// @return the unique key of the target entity, parsed from the source entity; this method is
    /// only called if [#isEntity(Object)] returns `true`.
    protected abstract K entityKeyFrom(S source);

    /// @param key    the unique key of the target entity; this is the same key that was returned by
    /// [#entityKeyFrom(Object)].
    /// @param source the source entity to create the target entity from.
    /// @return a new target entity, created from the given source.
    protected abstract T createEntityFrom(K key, S source);
}

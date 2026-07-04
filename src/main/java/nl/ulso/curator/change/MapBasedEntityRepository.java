package nl.ulso.curator.change;

import nl.ulso.curator.statistics.MeasurementCollector;
import nl.ulso.curator.statistics.MeasurementTracker;

import java.util.*;

import static java.util.Collections.*;
import static java.util.HashSet.newHashSet;
import static nl.ulso.curator.change.Change.updateWithObjectCast;

/// Base class for repositories that store entities in a map.
///
/// The repository is a [ChangeProcessor] that subscribes to changes of the supported entity type
/// and stores these entities in an internal map. Whenever the repository is updated, a single
/// update change is published to signal this update.
///
/// The internal map is mutated by the repository itself only. The API exposes only read-only views
/// to it. This guarantees that the state is consistent.
///
/// Statistics about the target entity are gathered automatically.
///
/// In a Dagger module, the subclass needs to be a singleton and registered twice: once as a
/// [ChangeProcessor], and once as a [MeasurementTracker]. In both cases the instances must be bound
/// into a set.
public abstract class MapBasedEntityRepository<K, E>
    extends EntityProcessor<E>
    implements MeasurementTracker
{
    private final Map<K, E> map = createMap();

    /// The class of this repository; an update change of this type is published whenever the
    /// repository is updated.
    protected abstract Class<?> repositoryClass();

    /// @param source the entity to create the key from.
    /// @return the unique key of the entity.
    protected abstract K entityKeyFrom(E source);

    @Override
    public final void reset()
    {
        map.clear();
    }

    @Override
    protected final void entityCreated(E newEntity, ChangeCollector collector)
    {
        map.put(entityKeyFrom(newEntity), newEntity);
        publishRepositoryUpdate(collector);
    }

    @Override
    protected final void entityUpdated(E oldEntity, E newEntity, ChangeCollector collector)
    {
        map.remove(entityKeyFrom(oldEntity));
        map.put(entityKeyFrom(newEntity), newEntity);
        publishRepositoryUpdate(collector);
    }

    @Override
    protected void entityDeleted(E oldEntity, ChangeCollector collector)
    {
        map.remove(entityKeyFrom(oldEntity));
        publishRepositoryUpdate(collector);
    }

    private void publishRepositoryUpdate(ChangeCollector collector)
    {
        var change = updateWithObjectCast(this, repositoryClass());
        collector.add(change);
    }

    @Override
    public final Collection<Change<?>> createChangeCollection()
    {
        return newHashSet(1);
    }

    @Override
    public final Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(repositoryClass());
    }

    @Override
    public final void collectMeasurements(MeasurementCollector collector)
    {
        collector.total(entityClass(), map.size());
    }

    /// @return a read-only view of the internal map.
    protected final Map<K, E> map()
    {
        return unmodifiableMap(map);
    }

    /// @return a read-only view of the internal map as a [NavigableMap].
    /// @throws ClassCastException if the implementation returned by [#createMap()] is not a
    /// [NavigableMap].
    protected final NavigableMap<K, E> navigableMap()
    {
        return unmodifiableNavigableMap((NavigableMap<K, E>) map);
    }

    /// @return a read-only view of the internal map as a [SortedMap].
    /// @throws ClassCastException if the implementation returned by [#createMap()] is not a
    /// [SortedMap].
    protected final SortedMap<K, E> sortedMap()
    {
        return unmodifiableSortedMap((SortedMap<K, E>) map);
    }

    /// @return a read-only view of the internal map as a [SequencedMap].
    /// @throws ClassCastException if the implementation returned by [#createMap()] is not a
    /// [SequencedMap].
    protected final SequencedMap<K, E> sequencedMap()
    {
        return unmodifiableSequencedMap((SequencedMap<K, E>) map);
    }

    /// @return the entity with the given key, if it exists.
    protected final Optional<E> findByKey(K key)
    {
        return Optional.ofNullable(map.get(key));
    }

    /// @return all entities in the repository.
    protected final Collection<E> entities()
    {
        return unmodifiableCollection(map.values());
    }

    /// @return the map to store entities in; by default, this is a [HashMap].
    protected Map<K, E> createMap()
    {
        return new HashMap<>();
    }

    @Override
    public final String name()
    {
        return repositoryClass().getSimpleName();
    }
}

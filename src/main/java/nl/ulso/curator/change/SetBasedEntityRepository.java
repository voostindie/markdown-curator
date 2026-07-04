package nl.ulso.curator.change;

import nl.ulso.curator.statistics.MeasurementCollector;
import nl.ulso.curator.statistics.MeasurementTracker;

import java.util.*;

import static java.util.Collections.unmodifiableNavigableSet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Collections.unmodifiableSortedSet;
import static java.util.HashSet.newHashSet;
import static nl.ulso.curator.change.Change.updateWithObjectCast;

/// Base class for repositories that store entities in a set.
///
/// The repository is a [ChangeProcessor] that subscribes to changes of the supported entity type
/// and stores these entities in an internal set. Whenever the repository is updated, a single
/// update change is published to signal this update.
///
/// The internal set is mutated by the repository itself only. The API exposes only read-only views
/// to it. This guarantees that the state is consistent.
///
/// Statistics about the target entity are gathered automatically.
///
/// In a Dagger module, the subclass needs to be a singleton and registered twice: once as a
/// [ChangeProcessor], and once as a [MeasurementTracker]. In both cases the instances must be bound
/// into a set.
public abstract class SetBasedEntityRepository<E>
    extends EntityProcessor<E>
    implements MeasurementTracker
{
    private final Set<E> set = createSet();

    /// The class of this repository; an update change of this type is published whenever the
    /// repository is updated.
    protected abstract Class<?> repositoryClass();

    @Override
    public final void reset()
    {
        set.clear();
    }

    @Override
    protected final void entityCreated(E newEntity, ChangeCollector collector)
    {
        set.add(newEntity);
        publishRepositoryUpdate(collector);
    }

    @Override
    protected final void entityUpdated(E oldEntity, E newEntity, ChangeCollector collector)
    {
        set.remove(oldEntity);
        set.add(newEntity);
        publishRepositoryUpdate(collector);
    }

    @Override
    protected final void entityDeleted(E oldEntity, ChangeCollector collector)
    {
        set.remove(oldEntity);
        publishRepositoryUpdate(collector);
    }

    @Override
    public final Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(repositoryClass());
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
    public final void collectMeasurements(MeasurementCollector collector)
    {
        collector.total(entityClass(), set.size());
    }

    /// @return a read-only view of the internal set.
    protected final Set<E> set()
    {
        return unmodifiableSet(set);
    }

    /// @return a read-only view of the internal set as a [NavigableSet].
    /// @throws ClassCastException if the implementation returned by [#createSet()] is not a
    /// [NavigableSet].
    protected final NavigableSet<E> navigableSet()
    {
        return unmodifiableNavigableSet((NavigableSet<E>) set);
    }

    /// @return a read-only view of the internal set as a [SortedSet].
    /// @throws ClassCastException if the implementation returned by [#createSet()] is not a
    /// [SortedSet].
    protected final SortedSet<E> sortedSet()
    {
        return unmodifiableSortedSet((SortedSet<E>) set);
    }

    /// @return the map to store entities in; by default, this is a [HashSet].
    protected Set<E> createSet()
    {
        return new HashSet<>();
    }

    @Override
    public final String name()
    {
        return repositoryClass().getSimpleName();
    }
}

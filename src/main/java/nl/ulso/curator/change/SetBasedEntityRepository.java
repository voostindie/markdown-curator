package nl.ulso.curator.change;

import nl.ulso.curator.statistics.MeasurementCollector;
import nl.ulso.curator.statistics.MeasurementTracker;
import nl.ulso.curator.vault.Vault;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.unmodifiableNavigableSet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Collections.unmodifiableSortedSet;
import static nl.ulso.curator.change.Change.*;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

/// Base class for repositories that transform source entities into target entities and store the
/// target entities in a set.
///
/// The repository is a [ChangeProcessor] that subscribes to changes of type [Vault] and the source
/// entity type. In case of a [Vault] event, the internal map is flushed. When it is a source entity
/// event, the source entity is mapped to a target entity if possible, the internal map is updated
/// accordingly, and a new event for the target entity type is published.
///
/// The internal set is mutated by the repository itself only. The API exposes only read-only views
/// it. This guarantees that the state is consistent.
///
/// Statistics about the target entity are gathered automatically.
///
/// In a Dagger module, the subclass needs to be a singleton and registered twice: once as a
/// [ChangeProcessor], and once as a [MeasurementTracker]. In both cases the instances must be bound
/// into a set.
public abstract class SetBasedEntityRepository<S, T>
    extends ChangeProcessorTemplate
    implements MeasurementTracker
{
    private final Set<T> set = createSet();

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
    protected final Set<? extends ChangeHandler> createChangeHandlers()
    {
        return Set.of(
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
        set.clear();
    }

    private void createEntity(Change<?> change, ChangeCollector collector)
    {
        var sourceEntity = change.as(sourceEntityClass()).newValue();
        var targetEntity = createEntityFrom(sourceEntity);
        set.add(targetEntity);
        collector.add(create(targetEntity, targetEntityClass()));
    }

    private void deleteEntity(Change<?> change, ChangeCollector collector)
    {
        var oldSourceEntity = change.as(sourceEntityClass()).oldValue();
        var oldTargetEntity = createEntityFrom(oldSourceEntity);
        set.remove(oldTargetEntity);
        collector.add(delete(oldTargetEntity, targetEntityClass()));
    }

    private void updateEntity(Change<?> change, ChangeCollector collector)
    {
        var oldSourceEntity = change.as(sourceEntityClass()).oldValue();
        var oldTargetEntity = createEntityFrom(oldSourceEntity);
        var newSourceEntity = change.as(sourceEntityClass()).newValue();
        if (isEntity(newSourceEntity))
        {
            // The new source entity represents a target entity...
            if (set.contains(oldTargetEntity))
            {
                // ...and so did the old source entity. So, we UPDATE the target entity.
                var newTargetEntity = createEntityFrom(newSourceEntity);
                set.remove(oldTargetEntity);
                set.add(newTargetEntity);
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
        collector.total(targetEntityClass(), set.size());
    }

    /// @return a read-only view of the internal set.
    protected final Set<T> set()
    {
        return unmodifiableSet(set);
    }

    /// @return a read-only view of the internal set as a [NavigableSet].
    /// @throws ClassCastException if the implementation returned by [#createSet()] is not a
    /// [NavigableSet].
    protected final NavigableSet<T> navigableSet()
    {
        return unmodifiableNavigableSet((NavigableSet<T>) set);
    }

    /// @return a read-only view of the internal set as a [SortedSet].
    /// @throws ClassCastException if the implementation returned by [#createSet()] is not a
    /// [SortedSet].
    protected final SortedSet<T> sortedSet()
    {
        return unmodifiableSortedSet((SortedSet<T>) set);
    }

    /// @return the map to store entities in; by default, this is a [HashSet].
    protected Set<T> createSet()
    {
        return new HashSet<>();
    }

    protected abstract Class<S> sourceEntityClass();

    protected abstract Class<T> targetEntityClass();

    /// @param source the source entity to check.
    /// @return whether the source entity represents a target entity.
    protected abstract boolean isEntity(S source);

    /// @param source the source entity to create the target entity from.
    /// @return a new target entity, created from the given source.
    protected abstract T createEntityFrom(S source);
}

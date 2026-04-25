package nl.ulso.curator.change;

import nl.ulso.curator.statistics.MeasurementCollector;
import nl.ulso.curator.statistics.MeasurementTracker;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.Vault;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.*;
import static nl.ulso.curator.change.Change.*;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

/// Base class for repositories that create entities from documents in the vault.
///
/// The repository maintains a map of entities. Whenever a document is created, updated or deleted
/// in the vault, applicable documents are mapped to entities. A new document results in a new
/// entity, a removed document results in a removed entity, and an updated document results in a new
/// entity to replace the old one (if any), or a deleted entity if the updated document no longer
/// represents an entity.
///
/// Every CREATE, UPDATE and DELETE of entities is published to the changelog.
///
/// Statistics about the entity are gathered automatically.
///
/// In a Dagger module, the subclass needs to be a singleton and registered twice: once as a
/// [ChangeProcessor], and once as a [MeasurementTracker]. In both cases the instances must be bound
/// into a set.
public abstract class DocumentBasedEntityRepository<K, E>
    extends ChangeProcessorTemplate
    implements MeasurementTracker
{
    private final Map<K, E> map = createMap();

    @Override
    public final Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Vault.class, Document.class);
    }

    @Override
    public final Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(entityClass());
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
        return isPayloadType(Document.class).and(
            change -> change.as(Document.class).values().anyMatch(this::isEntity));
    }

    /// TODO: as soon as the `init_event_replay` branch is merged, the collector will be removed.
    @Override
    protected final void reset(ChangeCollector collector)
    {
        map.clear();
        resetInternal(collector);
    }

    /// TODO: As soon as the `init_event_replay` branch is  merged, this method will be removed.
    protected void resetInternal(ChangeCollector collector)
    {
    }

    private void createEntity(Change<?> change, ChangeCollector collector)
    {
        var document = change.as(Document.class).newValue();
        var entity = createEntityFrom(document);
        map.put(entityKeyFrom(document), entity);
        collector.add(create(entity, entityClass()));
    }

    private void deleteEntity(Change<?> change, ChangeCollector collector)
    {
        var entity = map.remove(entityKeyFrom(change.as(Document.class).oldValue()));
        collector.add(delete(entity, entityClass()));
    }

    private void updateEntity(Change<?> change, ChangeCollector collector)
    {
        var newDocument = change.as(Document.class).newValue();
        var entityKey = entityKeyFrom(newDocument);
        if (isEntity(newDocument))
        {
            // The new document represents an entity...
            if (map.containsKey(entityKey))
            {
                // ...and so did the old document. So, we UPDATE the entity.
                var newEntity = createEntityFrom(newDocument);
                var oldEntity = map.put(entityKey, newEntity);
                collector.add(update(oldEntity, newEntity, entityClass()));
            }
            else
            {
                // ...but the old document did not. So, we CREATE a new entity.
                createEntity(change, collector);
            }
        }
        else
        {
            // The new document is not an entity, but the old document was.
            // So, we DELETE the old entity.
            deleteEntity(change, collector);
        }
    }

    @Override
    public void collectMeasurements(MeasurementCollector collector)
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

    /// @return the class of the entities in this repository.
    protected abstract Class<E> entityClass();

    /// @return whether the document represents an entity.
    protected abstract boolean isEntity(Document document);

    /// @return a new entity, created from the given document.
    protected abstract E createEntityFrom(Document document);

    /// @return the unique ID of the entity, parsed from the document.
    protected abstract K entityKeyFrom(Document document);
}

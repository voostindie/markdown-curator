package nl.ulso.curator.change;

import java.util.List;
import java.util.Set;

import static nl.ulso.curator.change.Change.isCreate;
import static nl.ulso.curator.change.Change.isDelete;
import static nl.ulso.curator.change.Change.isUpdate;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

/// Base class for change processors that process changes with a specific payload type `E`.
/// Subclasses can override the [#entityCreated(Object, ChangeCollector)],
/// [#entityUpdated(Object, Object, ChangeCollector)] and [#entityDeleted(Object, ChangeCollector)]
/// methods. The default implementations do nothing.
public abstract class EntityProcessor<E>
    extends ChangeProcessorTemplate
{
    protected abstract Class<E> entityClass();

    protected void entityCreated(E newEntity, ChangeCollector collector)
    {
    }

    protected void entityUpdated(E oldEntity, E newEntity, ChangeCollector collector)
    {
    }

    protected void entityDeleted(E oldEntity, ChangeCollector collector)
    {
    }

    @Override
    public final Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(entityClass());
    }

    @Override
    protected final List<? extends ChangeHandler> createChangeHandlers()
    {
        return List.of(
            newChangeHandler(
                isCreate(),
                (change, collector) ->
                    entityCreated(
                        change.as(entityClass()).newValue(),
                        collector
                    )
            ),
            newChangeHandler(
                isUpdate(),
                (change, collector) ->
                    entityUpdated(
                        change.as(entityClass()).oldValue(),
                        change.as(entityClass()).newValue(),
                        collector
                    )
            ),
            newChangeHandler(
                isDelete(),
                (change, collector) ->
                    entityDeleted(
                        change.as(entityClass()).oldValue(),
                        collector
                    )
            )
        );
    }
}

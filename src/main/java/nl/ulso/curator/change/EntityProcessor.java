package nl.ulso.curator.change;

import java.util.Set;

/// Base class for change processors that process entities of a specific type and that do not
/// keep any internal state.
public abstract class EntityProcessor<E>
    implements ChangeProcessor
{
    @Override
    public final Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(entityClass());
    }

    @Override
    public final Changelog apply(Changelog changelog)
    {
        var collector = new DefaultChangeCollector();
        changelog.changes()
            .map(change -> change.as(entityClass()))
            .forEach(change ->
            {
                switch (change.kind())
                {
                    case CREATE:
                        entityCreated(change.newValue(), collector);
                        break;
                    case UPDATE:
                        entityUpdate(change.oldValue(), change.newValue(), collector);
                        break;
                    case DELETE:
                        entityDeleted(change.oldValue(), collector);
                        break;
                }
            });
        return Changelog.emptyChangelog();
    }

    protected abstract Class<E> entityClass();

    protected void entityCreated(E newEntity, ChangeCollector collector)
    {
        // Do nothing by default.
    }

    protected void entityUpdate(E oldEntity, E newEntity, ChangeCollector collector)
    {
        // Do nothing by default.
    }

    protected void entityDeleted(E oldEntity, ChangeCollector collector)
    {
        // Do nothing by default.
    }
}

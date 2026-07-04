package nl.ulso.curator.change;

import java.util.Optional;
import java.util.Set;

/// Special processor that transforms entities of a type `S` into entities of a type `S`.
///
/// For every change with source payload type `S`, the change payload is transformed into a target
/// type `T` and then, if successful, published to the changelog.
///
/// Subclasses must implement [#transform(S)] to produce an optional `T`.
public abstract class EntityTransformer<S, T>
    extends EntityProcessor<S>
{
    protected abstract Class<S> sourceClass();

    protected abstract Class<T> targetClass();

    protected abstract Optional<T> transform(S source);

    @Override
    protected final Class<S> entityClass()
    {
        return sourceClass();
    }

    @Override
    public final Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(targetClass());
    }

    @Override
    protected final void entityCreated(S newSource, ChangeCollector collector)
    {
        transform(newSource).ifPresent(newTarget -> collector.create(newTarget, targetClass()));
    }

    @Override
    protected final void entityUpdated(S oldSource, S newSource, ChangeCollector collector)
    {
        var oldTarget = transform(oldSource);
        var newTarget = transform(newSource);
        if (oldTarget.isPresent() && newTarget.isPresent())
        {
            collector.update(oldTarget.get(), newTarget.get(), targetClass());
        }
        else if (newTarget.isPresent()) // && !oldTarget.isPresent()
        {
            collector.create(newTarget.get(), targetClass());
        }
        else if (oldTarget.isPresent()) // && !newTarget.isPresent()
        {
            collector.delete(oldTarget.get(), targetClass());
        }
    }

    @Override
    protected final void entityDeleted(S oldSource, ChangeCollector collector)
    {
        transform(oldSource).ifPresent(oldTarget -> collector.delete(oldTarget, targetClass()));
    }
}

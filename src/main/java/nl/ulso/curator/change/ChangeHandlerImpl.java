package nl.ulso.curator.change;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

final class ChangeHandlerImpl
    implements ChangeHandler
{
    private final Predicate<Change<?>> predicate;
    private final Function<Change<?>, Collection<Change<?>>> function;

    ChangeHandlerImpl(
        Predicate<Change<?>> predicate,
        Function<Change<?>, Collection<Change<?>>> function)
    {
        this.predicate = predicate;
        this.function = function;
    }

    @Override
    public boolean test(Change<?> change)
    {
        return predicate.test(change);
    }

    @Override
    public Collection<Change<?>> apply(Change<?> change)
    {
        return function.apply(change);
    }
}

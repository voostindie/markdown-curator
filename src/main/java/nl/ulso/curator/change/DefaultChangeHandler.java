package nl.ulso.curator.change;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

final class DefaultChangeHandler
    implements ChangeHandler
{
    private final Predicate<Change<?>> predicate;
    private final BiConsumer<Change<?>, ChangeCollector> consumer;

    DefaultChangeHandler(
        Predicate<Change<?>> predicate,
        BiConsumer<Change<?>, ChangeCollector> consumer)
    {
        this.predicate = predicate;
        this.consumer = consumer;
    }

    @Override
    public boolean test(Change<?> change)
    {
        return predicate.test(change);
    }

    @Override
    public void accept(Change<?> change, ChangeCollector collector)
    {
        consumer.accept(change, collector);
    }
}

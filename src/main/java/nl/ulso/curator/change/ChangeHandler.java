package nl.ulso.curator.change;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/// A handler for [Change<?>]s used by the [ChangeProcessorTemplate].
///
/// A handler is both a [Predicate] - to test whether it applies to a [Change<?>] - and a
/// [BiConsumer] that accepts a [Change<?>] to process and a [ChangeCollector] for producing new
/// changes, if any.
///
/// @see ChangeProcessorTemplate
public interface ChangeHandler
    extends Predicate<Change<?>>, BiConsumer<Change<?>, ChangeCollector>
{
    static ChangeHandler newChangeHandler(
        Predicate<Change<?>> predicate,
        BiConsumer<Change<?>, ChangeCollector> consumer)
    {
        return new DefaultChangeHandler(predicate, consumer);
    }
}

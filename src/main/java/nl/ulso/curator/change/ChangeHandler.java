package nl.ulso.curator.change;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

/// A handler for [Change<?>]s used by the [ChangeProcessorTemplate].
///
/// A handler is both a predicate - to test whether it applies to a [Change<?>] - and a function
/// that transforms a matching [Change<?>] into a list of new [Change<?>]s.
///
/// @see ChangeProcessorTemplate
public interface ChangeHandler
    extends Predicate<Change<?>>, Function<Change<?>, Collection<Change<?>>>
{
    static ChangeHandler newChangeHandler(
        Predicate<Change<?>> predicate,
        Function<Change<?>, Collection<Change<?>>> function)
    {
        return new DefaultChangeHandler(predicate, function);
    }
}

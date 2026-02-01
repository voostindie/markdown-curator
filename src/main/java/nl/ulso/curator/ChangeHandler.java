package nl.ulso.curator;

import nl.ulso.curator.changelog.Change;

import java.util.Collection;
import java.util.function.Function;

/// A more concise interface for change handlers: a change handler is a function that takes a change
/// and returns a collection of changes, possibly empty.
public interface ChangeHandler
    extends Function<Change<?>, Collection<Change<?>>>
{
}

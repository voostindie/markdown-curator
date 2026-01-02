package nl.ulso.markdown_curator;

import java.util.Collection;
import java.util.function.Function;

/// A more concise interface for change handlers.
public interface ChangeHandler
    extends Function<Change<?>, Collection<Change<?>>>
{
}

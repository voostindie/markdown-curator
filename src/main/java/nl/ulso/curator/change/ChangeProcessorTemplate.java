package nl.ulso.curator.change;

import java.util.*;

/// Base class for [ChangeProcessor]s that processes [Changelog]s through [ChangeHandler]s.
///
/// Subclasses must register one or more [ChangeHandler]s by overriding the
/// [#createChangeHandlers()] method.
///
/// Notes:
///
/// - Changes are processed fully, in order.
/// - The same change can be accepted by multiple [ChangeHandler]s
/// - The order in which handlers are executed is guaranteed.
public abstract class ChangeProcessorTemplate
    implements ChangeProcessor
{
    private final List<ChangeHandler> changeHandlers;

    protected ChangeProcessorTemplate()
    {
        this.changeHandlers = List.copyOf(createChangeHandlers());
        if (this.changeHandlers.isEmpty())
        {
            throw new IllegalStateException(
                "No change handlers configured. This processor is useless!"
            );
        }
    }

    protected abstract List<? extends ChangeHandler> createChangeHandlers();

    @Override
    public final Changelog apply(Changelog changelog)
    {
        var collector = new DefaultChangeCollector(createChangeCollection());
        changelog.changes().forEach(change ->
            changeHandlers.stream()
                .filter(handler -> handler.test(change))
                .forEach(handler -> handler.accept(change, collector))
        );
        return collector.changelog();
    }


    /// Creates the collection to capture changes in. The default implementation creates an
    /// [ArrayList].
    protected Collection<Change<?>> createChangeCollection()
    {
        return new ArrayList<>();
    }
}

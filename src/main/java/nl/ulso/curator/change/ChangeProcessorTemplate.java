package nl.ulso.curator.change;

import nl.ulso.curator.vault.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.Collections.emptySet;

/// Base class for [ChangeProcessor]s that processes [Changelog]s through [ChangeHandler]s.
///
/// Subclasses can do an optional full reset of their internal state followed by fine-grained change
/// processing for each applicable change in the changelog.
///
/// A reset is executed whenever [#isResetRequired(Changelog)] returns `true`.
///
/// To perform fine-grained  change processing, subclasses need to register one or more
/// [ChangeHandler]s by overriding the [#createChangeHandlers()] method.
///
/// When performing a full reset or running a [ChangeHandler], new changes can be produced to the
/// changelog through the [ChangeCollector].
///
/// Note:
///
/// - A reset, if applicable, always comes before fine-grained change processing, independent of the
/// order of the changes available in the changelog.
/// - The same change can be accepted by multiple [ChangeHandler]s
/// - The order in which handlers are executed is not guaranteed.
public abstract class ChangeProcessorTemplate
    implements ChangeProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeProcessorTemplate.class);

    private final Set<ChangeHandler> changeHandlers;

    protected ChangeProcessorTemplate()
    {
        this.changeHandlers = Set.copyOf(createChangeHandlers());
        if (this.changeHandlers.isEmpty())
        {
            LOGGER.debug("No change handlers configured for change processor: {}.",
                this.getClass().getSimpleName()
            );
        }
    }

    protected Set<? extends ChangeHandler> createChangeHandlers()
    {
        return emptySet();
    }

    /// Creates the collection to capture the changes of the various change handlers in. The default
    /// implementation creates an [ArrayList]. Maybe a subclass wants to collect unique changes
    /// only. In such a case: override this method to return a set.
    protected Collection<Change<?>> createChangeCollection()
    {
        return new ArrayList<>();
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Vault.class, Document.class, Folder.class);
    }

    @Override
    public final Changelog apply(Changelog changelog)
    {
        var changeCollector = new DefaultChangeCollector(createChangeCollection());
        if (isResetRequired(changelog))
        {
            LOGGER.debug(
                "Performing a reset on change processor: {}.",
                this.getClass().getSimpleName()
            );
            reset(changeCollector);
        }
        if (!changeHandlers.isEmpty())
        {
            LOGGER.debug("Processing the changelog on change processor: {}.",
                this.getClass().getSimpleName()
            );
            process(changelog, changeCollector);
        }
        return changeCollector.changelog();
    }

    /// Determines whether a reset is required from this changelog.
    ///
    /// The default implementation checks for the existence of a change to the [Vault].
    protected boolean isResetRequired(Changelog changelog)
    {
        return changelog.changesFor(Vault.class).findAny().isPresent();
    }

    /// Performs a reset.
    protected void reset(ChangeCollector collector)
    {
        throw new IllegalStateException(
            "A reset is triggered, but not supported by this change processor: " +
            getClass().getSimpleName());
    }

    /// Processes the changes in the changelog one by one, in order.
    ///
    /// Every change in the changelog is tested against each of the change handlers. When tested
    /// positively, the change handler is requested to handle (accept) the change.
    private void process(Changelog changelog, ChangeCollector collector)
    {
        changelog.changes().forEach(change ->
            changeHandlers.stream()
                .filter(handler -> handler.test(change))
                .forEach(handler -> handler.accept(change, collector))
        );
    }
}

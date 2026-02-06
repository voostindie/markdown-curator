package nl.ulso.curator.change;

import nl.ulso.curator.vault.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.Collections.emptySet;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.change.Changelog.emptyChangelog;

/// Base class for [ChangeProcessor]s that processes [Changelog]s through [ChangeHandler]s.
///
/// Subclasses either process the changes in a changelog, or do a reset of their internal data
/// structures. A reset is executed whenever [#isResetRequired(Changelog)] returns `true`.
///
/// To perform actual change processing, subclasses need to register one or more [ChangeHandler]s
/// by overriding the [#createChangeHandlers()] method.
///
/// Note that the same change can be accepted by multiple [ChangeHandler]s, and that the order in
/// which handlers are executed is not guaranteed.
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

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Vault.class, Document.class, Folder.class);
    }

    @Override
    public final Changelog apply(Changelog changelog)
    {
        if (isResetRequired(changelog))
        {
            LOGGER.debug(
                "Performing a reset on change processor: {}.",
                this.getClass().getSimpleName()
            );
            return changelogFor(reset());
        }
        if (!changeHandlers.isEmpty())
        {
            LOGGER.debug("Processing the changelog on change processor: {}.",
                this.getClass().getSimpleName()
            );
            return changelogFor(process(changelog));
        }
        LOGGER.debug("Nothing to do for change processor: {}.", this.getClass().getSimpleName());
        return emptyChangelog();
    }

    /// Determines whether a reset is required from this changelog.
    ///
    /// The default implementation checks for the existence of a change to the [Vault].
    protected boolean isResetRequired(Changelog changelog)
    {
        return changelog.changesFor(Vault.class).findAny().isPresent();
    }

    /// Performs a reset.
    protected Collection<Change<?>> reset()
    {
        throw new IllegalStateException(
            "A reset is triggered, but not supported by this change processor: " +
            getClass().getSimpleName());
    }

    /// Processes the changelog.
    ///
    /// Every change in the changelog is matched to each of the predicates in the map of change
    /// handlers. When the predicate evaluates to `true`, the associated change handler is
    /// executed.
    private Collection<Change<?>> process(Changelog changelog)
    {
        var changes = createChangeCollection();
        changelog.changes().forEach(change -> {
                for (ChangeHandler handler : changeHandlers)
                {
                    if (handler.test(change))
                    {
                        changes.addAll(handler.apply(change));
                    }
                }
            }
        );
        return changes;
    }

    /// Creates the collection to capture the changes of the various change handlers in. The default
    /// implementation creates an [ArrayList]. Maybe a subclass wants to collect unique changes
    /// only. In such a case: override this method to return a set.
    protected Collection<Change<?>> createChangeCollection()
    {
        return new ArrayList<>();
    }
}

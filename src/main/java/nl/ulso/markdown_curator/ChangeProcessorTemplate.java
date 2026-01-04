package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

import static nl.ulso.markdown_curator.Change.Kind.CREATE;
import static nl.ulso.markdown_curator.Change.Kind.DELETE;
import static nl.ulso.markdown_curator.Change.Kind.UPDATE;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static nl.ulso.markdown_curator.Changelog.emptyChangelog;

/// Base class for [ChangeProcessor] that can handle granular change events.
///
/// Change processors either process the changes in a changelog all at once or one by one. A full
/// refresh is executed whenever [#isFullRefreshRequired(Changelog)] returns `true`.
///
/// In order to support granular change processing, subclasses need to register one or more
/// predicates to test changes against, and a change handler for each predicate.
///
/// Note that the same change can be accepted by multiple predicates, and that the order in which
/// change handlers are executed is not guaranteed.
///
/// To aid in the definition of predicates, this template class contains a number of factory methods
/// for predicates that can be combined at will.
public abstract class ChangeProcessorTemplate
    implements ChangeProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeProcessorTemplate.class);

    private final Map<Predicate<Change<?>>, ChangeHandler> changeHandlers = new HashMap<>();

    @Override
    public final Changelog run(Changelog changelog)
    {
        if (isFullRefreshRequired(changelog))
        {
            LOGGER.debug(
                "Performing a full refresh on change processor: {}",
                this.getClass().getSimpleName()
            );
            return changelogFor(fullRefresh());
        }
        if (!changeHandlers.isEmpty())
        {
            LOGGER.debug("Performing an incremental refresh on change processor: {}",
                this.getClass().getSimpleName()
            );
            return changelogFor(incrementalRefresh(changelog));
        }
        LOGGER.debug("Nothing to do for change processor: {}", this.getClass().getSimpleName());
        return emptyChangelog();
    }

    protected final void registerChangeHandler(
        Predicate<Change<?>> predicate,
        ChangeHandler handler)
    {
        changeHandlers.put(predicate, handler);
    }

    protected final Predicate<Change<?>> isObjectType(Class<?> objectType)
    {
        return change -> change.objectType().equals(objectType);
    }

    protected final Predicate<Change<?>> isCreate()
    {
        return change -> change.kind() == CREATE;
    }

    protected final Predicate<Change<?>> isUpdate()
    {
        return change -> change.kind() == UPDATE;
    }

    protected final Predicate<Change<?>> isCreateOrUpdate()
    {
        return isCreate().or(isUpdate());
    }

    protected final Predicate<Change<?>> isDelete()
    {
        return change -> change.kind() == DELETE;
    }

    /// Determines whether a full refresh is required based on the changelog.
    ///
    /// The default implementation checks for the existence of a change to the [Vault].
    protected boolean isFullRefreshRequired(Changelog changelog)
    {
        return changelog.changesFor(Vault.class).findAny().isPresent();
    }

    /// Performs a full refresh.
    protected Collection<Change<?>> fullRefresh()
    {
        throw new IllegalStateException(
            "A full refresh is triggered, but not supported by this change processor: " +
            getClass().getSimpleName());
    }

    /// Performs an incremental refresh, based on introspection of the changelog.
    ///
    /// Every change in the changelog is matched to each of the predicates in the map of change
    /// handlers. When the predicate evaluates to `true`, the associated change handler is
    /// executed.
    private Collection<Change<?>> incrementalRefresh(Changelog changelog)
    {
        var changes = createChangeCollection();
        changelog.changes().forEach(
            change -> changeHandlers.forEach(
                (predicate, handler) ->
                {
                    if (predicate.test(change))
                    {
                        changes.addAll(handler.apply(change));
                    }
                }
            )
        );
        return changes;
    }

    /// Creates the collection to capture the changes of the various change handlers in.
    protected Collection<Change<?>> createChangeCollection()
    {
        return new ArrayList<>();
    }
}

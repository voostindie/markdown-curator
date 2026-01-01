package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.Folder;
import nl.ulso.markdown_curator.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static nl.ulso.markdown_curator.Change.Kind.CREATION;
import static nl.ulso.markdown_curator.Change.Kind.DELETION;
import static nl.ulso.markdown_curator.Change.Kind.MODIFICATION;
import static nl.ulso.markdown_curator.Changelog.changelogFor;

/// Base class for [DataModel] that can handle granular change events.
///
/// Data models are either refreshed fully or granurarly, based on the incoming changes. To handle
/// granular changes, subclasses need to register one or more predicates to test changes against,
/// and a change handler for each predicate.
///
/// Note that the same change can be accepted by multiple predicates!
public abstract class DataModelTemplate
    implements DataModel
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DataModelTemplate.class);

    private final Map<Predicate<Change<?>>, Function<Change<?>, Collection<Change<?>>>>
        changeHandlers = new HashMap<>();

    @Override
    public final Changelog process(Changelog changelog)
    {
        if (isFullRefreshRequired(changelog))
        {
            LOGGER.debug(
                "Performing a full refresh on data model: {}",
                this.getClass().getSimpleName()
            );
            return changelogFor(fullRefresh());
        }
        LOGGER.debug("Performing an incremental refresh on data model: {}",
            this.getClass().getSimpleName()
        );
        return changelogFor(incrementalRefresh(changelog));
    }

    protected final void registerChangeHandler(
        Predicate<Change<?>> predicate,
        Function<Change<?>, Collection<Change<?>>> handler)
    {
        changeHandlers.put(predicate, handler);
    }

    protected Predicate<Change<?>> hasObjectType(Class<?> objectType)
    {
        return change -> change.objectType().equals(objectType);
    }

    protected Predicate<Change<?>> isCreation()
    {
        return change -> change.kind() == CREATION;
    }

    protected Predicate<Change<?>> isModification()
    {
        return change -> change.kind() == MODIFICATION;
    }

    protected Predicate<Change<?>> isCreationOrModification()
    {
        return isCreation().or(isModification());
    }

    protected Predicate<Change<?>> isDeletion()
    {
        return change -> change.kind() == DELETION;
    }

    protected Function<Change<?>, Collection<Change<?>>> fullRefreshHandler()
    {
        return _ ->
        {
            LOGGER.debug(
                "Incremental change is leading to a full refresh on data model: {}",
                this.getClass().getSimpleName()
            );
            return fullRefresh();
        };
    }

    protected boolean isInHierarchyOf(Vault vault, Folder parent, Folder child)
    {
        var folder = child;
        while (folder != parent)
        {
            if (folder == vault)
            {
                return false;
            }
            folder = folder.parent();
        }
        return true;
    }

    /// A full refresh of the data model is required if the list of change handlers is empty - in
    /// other words: an incremental update wouldn't do anything - or the changelog contains a change
    /// for a [Vault] object.
    private boolean isFullRefreshRequired(Changelog changelog)
    {
        return changeHandlers.isEmpty() || changelog.changesFor(Vault.class).findAny().isPresent();
    }

    /// Performs a full refresh of the data model.
    public abstract Collection<Change<?>> fullRefresh();

    /// Performs an incremental refresh of the data model, based on introspection of the changelog.
    private Collection<Change<?>> incrementalRefresh(Changelog changelog)
    {
        var changes = new ArrayList<Change<?>>();
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
}

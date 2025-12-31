package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.vaultRefreshed;

/**
 * Base class for {@link DataModel} that can handle granular change events.
 * <p/>
 * The default implementation for each change event is to dispatch to
 * {@link #process(VaultRefreshed, Changelog)}, which in turn eventually calls
 * {@link #fullRefresh(Changelog)}. In other words, just implementing
 * {@link #fullRefresh(Changelog)} already works for all events. Override one of the
 * other {@code process} methods to make the refresh more granular and efficient for that
 * specific event.
 * <p/>
 * A curator can have many models and models may depend on one another. That means models needs
 * to be refreshed in the right order. See {@link Curator#orderDataModels(Set)}.
 */
public abstract class DataModelTemplate
        implements DataModel, VaultChangedEventHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DataModelTemplate.class);

    @Override
    public final Changelog vaultChanged(VaultChangedEvent event, Changelog changelog)
    {
        // Look ma, no instanceof!
        return event.dispatch(this, changelog);
    }

    @Override
    public final Changelog process(VaultRefreshed event, Changelog changelog)
    {
        LOGGER.debug("Performing a full refresh on data model: {}",
            this.getClass().getSimpleName()
        );
        return fullRefresh(changelog);
    }

    /**
     * Fully refreshes the data model from the vault.
     */
    public abstract Changelog fullRefresh(Changelog changelog);

    @Override
    public Changelog process(FolderAdded event, Changelog changelog)
    {
        return process(vaultRefreshed(), changelog);
    }

    @Override
    public Changelog process(FolderRemoved event, Changelog changelog)
    {
        return process(vaultRefreshed(), changelog);
    }

    @Override
    public Changelog process(DocumentAdded event, Changelog changelog)
    {
        return process(vaultRefreshed(), changelog);
    }

    @Override
    public Changelog process(DocumentChanged event, Changelog changelog)
    {
        return process(vaultRefreshed(), changelog);
    }

    @Override
    public Changelog process(DocumentRemoved event, Changelog changelog)
    {
        return process(vaultRefreshed(), changelog);
    }

    @Override
    public Changelog process(ExternalChange event, Changelog changelog)
    {
        // Do nothing by default, since no content in the vault itself changed.
        return changelog;
    }
}

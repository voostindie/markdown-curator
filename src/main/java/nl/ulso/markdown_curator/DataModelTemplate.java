package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.vaultRefreshed;

/**
 * Base class for {@link DataModel} that can handle granular event change events.
 * <p/>
 * The default implementation for each change event is to dispatch to
 * {@link #process(VaultRefreshed)}, which in turns call {@link #fullRefresh()}. In other words,
 * just implementing {@link #fullRefresh()} already works for all events. Override one of the
 * other {@code process} methods to make the refresh more granular and efficient for that
 * specific event.
 */

public abstract class DataModelTemplate
        implements DataModel, VaultChangedEventHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DataModelTemplate.class);

    /**
     * Fully refreshes the data model from the vault.
     */
    protected abstract void fullRefresh();

    @Override
    public final void vaultChanged(VaultChangedEvent event)
    {
        // Look ma, no instanceof!
        event.dispatch(this);
    }

    @Override
    public final void process(VaultRefreshed event)
    {
        LOGGER.debug("Performing a full refresh on data model: {}",
                this.getClass().getSimpleName());
        fullRefresh();
    }

    @Override
    public void process(FolderAdded event)
    {
        process(vaultRefreshed());
    }

    @Override
    public void process(FolderRemoved event)
    {
        process(vaultRefreshed());
    }

    @Override
    public void process(DocumentAdded event)
    {
        process(vaultRefreshed());
    }

    @Override
    public void process(DocumentChanged event)
    {
        process(vaultRefreshed());
    }

    @Override
    public void process(DocumentRemoved event)
    {
        process(vaultRefreshed());
    }
}

package nl.ulso.markdown_curator.vault.event;

/**
 * Event triggered by the {@link nl.ulso.markdown_curator.vault.VaultRefresher} for changes detected
 * in external sources.
 */
public final class ExternalChange
    implements VaultChangedEvent
{
    static final ExternalChange INSTANCE = new ExternalChange();

    private ExternalChange()
    {
    }

    @Override
    public void dispatch(VaultChangedEventHandler handler)
    {
        handler.process(this);
    }
}

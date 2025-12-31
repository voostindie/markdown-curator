package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.Changelog;

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
    public Changelog dispatch(VaultChangedEventHandler handler, Changelog changelog)
    {
        return handler.process(this, changelog);
    }
}

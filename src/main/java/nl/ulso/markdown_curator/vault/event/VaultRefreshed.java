package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.Changelog;

public final class VaultRefreshed
        implements VaultChangedEvent
{
    static final VaultRefreshed INSTANCE = new VaultRefreshed();

    private VaultRefreshed()
    {
    }

    @Override
    public Changelog dispatch(VaultChangedEventHandler handler, Changelog changelog)
    {
        return handler.process(this, changelog);
    }
}

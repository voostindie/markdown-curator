package nl.ulso.markdown_curator.vault.event;

public final class VaultRefreshed
        implements VaultChangedEvent
{
    static final VaultRefreshed INSTANCE = new VaultRefreshed();

    private VaultRefreshed()
    {
    }

    @Override
    public void dispatch(VaultChangedEventHandler handler)
    {
        handler.process(this);
    }
}

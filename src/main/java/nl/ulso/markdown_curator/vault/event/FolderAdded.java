package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.vault.Folder;

public final class FolderAdded
        implements VaultChangedEvent
{
    private final Folder folder;

    FolderAdded(Folder folder) {
        this.folder = folder;
    }

    @Override
    public void dispatch(VaultChangedEventHandler handler)
    {
        handler.process(this);
    }

    public Folder folder()
    {
        return folder;
    }
}

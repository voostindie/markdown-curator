package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.vault.Folder;

public final class FolderRemoved
        implements VaultChangedEvent
{
    private final Folder folder;

    FolderRemoved(Folder folder)
    {
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

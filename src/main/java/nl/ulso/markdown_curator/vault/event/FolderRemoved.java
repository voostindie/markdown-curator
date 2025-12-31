package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.Changelog;
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
    public Changelog dispatch(VaultChangedEventHandler handler, Changelog changelog)
    {
        return handler.process(this, changelog);
    }

    public Folder folder()
    {
        return folder;
    }
}

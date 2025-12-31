package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.vault.Folder;

public final class FolderAdded
        implements VaultChangedEvent
{
    private final Folder folder;

    FolderAdded(Folder folder)
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

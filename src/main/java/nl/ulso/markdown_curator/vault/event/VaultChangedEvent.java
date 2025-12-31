package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.vault.Document;
import nl.ulso.markdown_curator.vault.Folder;

/**
 * Defines the nature of the event that happened to the vault. Each event also holds a reference
 * to its subject (a folder or a document).
 * <p/>
 * The set of supported events is fixed.
 * <p/>
 * The event's type defines what actually happened. Implement the
 * {@link nl.ulso.markdown_curator.vault.event.VaultChangedEventHandler} to have the events use
 * their double dispatching capability.
 */
public sealed interface VaultChangedEvent
        permits VaultRefreshed, FolderAdded, FolderRemoved, DocumentAdded, DocumentChanged,
        DocumentRemoved, ExternalChange
{
    Changelog dispatch(VaultChangedEventHandler handler, Changelog changelog);

    static VaultRefreshed vaultRefreshed()
    {
        return VaultRefreshed.INSTANCE;
    }

    static FolderAdded folderAdded(Folder folder)
    {
        return new FolderAdded(folder);
    }

    static FolderRemoved folderRemoved(Folder folder)
    {
        return new FolderRemoved(folder);
    }

    static DocumentAdded documentAdded(Document document)
    {
        return new DocumentAdded(document);
    }

    static DocumentChanged documentChanged(Document document)
    {
        return new DocumentChanged(document);
    }

    static DocumentRemoved documentRemoved(Document document)
    {
        return new DocumentRemoved(document);
    }

    static ExternalChange externalChange()
    {
        return ExternalChange.INSTANCE;
    }
}

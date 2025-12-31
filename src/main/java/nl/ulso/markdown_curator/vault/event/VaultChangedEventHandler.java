package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.Changelog;

/**
 * Supports double dispatching for {@link VaultChangedEvent}s through their
 * {@link VaultChangedEvent#dispatch(VaultChangedEventHandler, Changelog)} method.
 */
public interface VaultChangedEventHandler
{
    Changelog process(VaultRefreshed event, Changelog changelog);

    Changelog process(FolderAdded event, Changelog changelog);

    Changelog process(FolderRemoved event, Changelog changelog);

    Changelog process(DocumentAdded event, Changelog changelog);

    Changelog process(DocumentChanged event, Changelog changelog);

    Changelog process(DocumentRemoved event, Changelog changelog);

    Changelog process(ExternalChange event, Changelog changelog);
}

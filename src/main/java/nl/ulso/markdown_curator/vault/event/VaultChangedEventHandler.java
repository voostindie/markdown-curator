package nl.ulso.markdown_curator.vault.event;

/**
 * Supports double dispatching for {@link VaultChangedEvent}s through their
 * {@link VaultChangedEvent#dispatch(VaultChangedEventHandler)} method.
 */
public interface VaultChangedEventHandler
{
    void process(VaultRefreshed event);

    void process(FolderAdded event);

    void process(FolderRemoved event);

    void process(DocumentAdded event);

    void process(DocumentChanged event);

    void process(DocumentRemoved event);

    void process(ExternalChange event);
}

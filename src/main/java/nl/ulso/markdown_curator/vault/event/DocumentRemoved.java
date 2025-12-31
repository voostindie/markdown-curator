package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.vault.Document;

public final class DocumentRemoved
        implements VaultChangedEvent
{
    private final Document document;

    DocumentRemoved(Document document)
    {
        this.document = document;
    }

    @Override
    public Changelog dispatch(VaultChangedEventHandler handler, Changelog changelog)
    {
        return handler.process(this, changelog);
    }

    public Document document()
    {
        return document;
    }
}

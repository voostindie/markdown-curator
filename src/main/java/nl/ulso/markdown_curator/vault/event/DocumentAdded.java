package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.vault.Document;

public final class DocumentAdded
        implements VaultChangedEvent
{
    private final Document document;

    DocumentAdded(Document document)
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

package nl.ulso.markdown_curator.vault.event;

import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.vault.Document;

public final class DocumentChanged
        implements VaultChangedEvent
{
    private final Document document;

    DocumentChanged(Document document)
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

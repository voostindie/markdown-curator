package nl.ulso.markdown_curator.vault.event;

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
    public void dispatch(VaultChangedEventHandler handler)
    {
        handler.process(this);
    }

    public Document document()
    {
        return document;
    }
}

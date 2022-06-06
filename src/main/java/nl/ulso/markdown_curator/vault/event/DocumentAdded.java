package nl.ulso.markdown_curator.vault.event;

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
    public void dispatch(VaultChangedEventHandler handler)
    {
        handler.process(this);
    }

    public Document document()
    {
        return document;
    }
}

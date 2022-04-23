package nl.ulso.macu.vault;

import static java.util.Objects.requireNonNull;

abstract class DocumentHolder
{
    private Document document;

    public Document document() {
        return document;
    }

    void setDocument(Document document)
    {
        if (this.document != null)
        {
            throw new AssertionError("Document can be set at most once");
        }
        this.document = requireNonNull(document);
    }
}

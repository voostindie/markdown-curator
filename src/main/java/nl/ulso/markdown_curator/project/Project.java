package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.Document;

/**
 * Simple wrapper around {@link Document} to have projects as a dedicated type in the system.
 */
public final class Project
{
    private final Document document;

    Project(Document document)
    {
        this.document = document;
    }

    public Document document()
    {
        return document;
    }

    public String name()
    {
        return document.name();
    }
}

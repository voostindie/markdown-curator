package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.Document;

public record Project(Document document)
{
    public String name()
    {
        return document.name();
    }
}

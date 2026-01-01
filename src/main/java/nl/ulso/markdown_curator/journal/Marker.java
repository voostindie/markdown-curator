package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.vault.Dictionary;
import nl.ulso.markdown_curator.vault.Document;

public record Marker(Document document)
{
    public String name()
    {
        return document.name();
    }

    public Dictionary settings()
    {
        return document.frontMatter();
    }
}

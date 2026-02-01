package nl.ulso.curator.journal;

import nl.ulso.curator.vault.Dictionary;
import nl.ulso.curator.vault.Document;

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

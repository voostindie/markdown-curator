package nl.ulso.curator.addon.journal;

import nl.ulso.dictionary.Dictionary;
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

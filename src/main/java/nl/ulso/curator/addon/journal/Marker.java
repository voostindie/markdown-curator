package nl.ulso.curator.addon.journal;

import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;

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

    @Override
    public String toString()
    {
        return name();
    }
}

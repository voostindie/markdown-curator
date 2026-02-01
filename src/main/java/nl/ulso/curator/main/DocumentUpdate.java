package nl.ulso.curator.main;

import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;

import java.util.List;

public record DocumentUpdate(
    Document document, Dictionary frontMatter, List<QueryOutput> queryOutputs)
{
}

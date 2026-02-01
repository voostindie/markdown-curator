package nl.ulso.curator;

import nl.ulso.curator.vault.Dictionary;
import nl.ulso.curator.vault.Document;

import java.util.List;

public record DocumentUpdate(Document document, Dictionary frontMatter, List<QueryOutput> queryOutputs)
{
}

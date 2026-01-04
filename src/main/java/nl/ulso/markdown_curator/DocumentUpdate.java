package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.Dictionary;
import nl.ulso.markdown_curator.vault.Document;

import java.util.List;

public record DocumentUpdate(Document document, Dictionary frontMatter, List<QueryOutput> queryOutputs)
{
}

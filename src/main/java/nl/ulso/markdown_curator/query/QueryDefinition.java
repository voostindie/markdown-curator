package nl.ulso.markdown_curator.query;

import nl.ulso.markdown_curator.vault.Dictionary;
import nl.ulso.markdown_curator.vault.Document;

public interface QueryDefinition
{
    String queryName();

    Dictionary configuration();

    Document document();

    String result();
}

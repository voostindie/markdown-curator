package nl.ulso.curator.query;

import nl.ulso.curator.vault.Dictionary;
import nl.ulso.curator.vault.Document;

public interface QueryDefinition
{
    String queryName();

    Dictionary configuration();

    Document document();

    String outputHash();
}

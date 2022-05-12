package nl.ulso.markdown_curator.query;

import nl.ulso.markdown_curator.vault.QueryBlock;

import java.util.Map;

public interface Query
{
    String name();

    String description();

    Map<String, String> supportedConfiguration();

    QueryResult run(QueryBlock queryBlock);
}

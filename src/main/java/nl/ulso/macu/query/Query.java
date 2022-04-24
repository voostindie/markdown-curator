package nl.ulso.macu.query;

import nl.ulso.macu.vault.QueryBlock;

import java.util.Map;

public interface Query
{
    String name();

    String description();

    Map<String, String> supportedConfiguration();

    QueryResult run(QueryBlock queryBlock);
}

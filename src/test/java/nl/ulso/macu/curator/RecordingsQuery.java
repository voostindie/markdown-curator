package nl.ulso.macu.curator;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.QueryBlock;
import nl.ulso.macu.vault.Vault;

import java.util.Map;

import static nl.ulso.macu.query.QueryResult.failure;

public class RecordingsQuery
        implements Query
{
    private final Vault vault;

    public RecordingsQuery(Vault vault)
    {
        this.vault = vault;
    }

    @Override
    public String name()
    {
        return "recordings";
    }

    @Override
    public String description()
    {
        return "lists all recordings of a song";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("song", "Name of the song. Defaults to document name.");
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        return failure("Not implemented yet");
    }
}

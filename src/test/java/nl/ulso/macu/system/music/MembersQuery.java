package nl.ulso.macu.system.music;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.QueryBlock;
import nl.ulso.macu.vault.Vault;

import java.util.Map;

import static nl.ulso.macu.query.QueryResult.failure;

public class MembersQuery
        implements Query
{
    private final Vault vault;

    public MembersQuery(Vault vault)
    {
        this.vault = vault;
    }

    @Override
    public String name()
    {
        return "members";
    }

    @Override
    public String description()
    {
        return "lists all members of a band";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("artist", "Name of the band. Defaults to document name.");
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        return failure("Not implemented yet");
    }
}

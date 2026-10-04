package nl.ulso.curator.server;

import nl.ulso.curator.Curator;
import nl.ulso.curator.RunMode;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Vault;
import nl.ulso.curator.vault.VaultStub;

import java.util.*;

import static java.util.Collections.unmodifiableSet;
import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;

public class CuratorStub
    implements Curator
{
    private final Vault vault;
    private final Map<String, Query> queries;
    private final Set<OutputFormat> outputFormats;
    private boolean failSupports = false;

    public CuratorStub()
    {
        this(new VaultStub(), new HashMap<>(), Set.of(JSON, MARKDOWN));
    }

    public CuratorStub(Vault vault)
    {
        this(vault, new HashMap<>(), Set.of(JSON, MARKDOWN));
    }

    public CuratorStub(Vault vault, Map<String, Query> queries, Set<OutputFormat> outputFormats)
    {
        this.vault = vault;
        this.queries = queries;
        this.outputFormats = new HashSet<>(outputFormats);
    }

    public void registerQuery(Query query)
    {
        queries.put(query.name(), query);
    }

    public void setFailSupports(boolean failSupports)
    {
        this.failSupports = failSupports;
    }

    @Override
    public void run(RunMode runMode)
    {
        // No-op for tests
    }

    @Override
    public Vault vault()
    {
        return vault;
    }

    @Override
    public Query query(String queryName)
    {
        var query = queries.get(queryName);
        if (query == null)
        {
            throw new IllegalArgumentException("Unknown query: " + queryName);
        }
        return query;
    }

    @Override
    public Set<OutputFormat> outputFormats()
    {
        return unmodifiableSet(outputFormats);
    }

    @Override
    public boolean supports(QueryResult queryResult, OutputFormat outputFormat)
    {
        if (failSupports)
        {
            return false;
        }
        return outputFormats.contains(outputFormat);
    }

    @Override
    public String format(QueryResult result, OutputFormat format)
    {
        return "formatted:" + format.name();
    }
}

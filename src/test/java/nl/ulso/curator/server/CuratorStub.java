package nl.ulso.curator.server;

import nl.ulso.curator.Curator;
import nl.ulso.curator.RunMode;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Vault;
import nl.ulso.curator.vault.VaultStub;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.util.Collections.unmodifiableSet;
import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;

public class CuratorStub
    implements Curator
{
    private final Vault vault;
    private final Map<String, Query> queries;
    private final Set<OutputFormat> outputFormats;
    private volatile boolean fallBackToMarkdown = false;

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
        this.outputFormats = new CopyOnWriteArraySet<>(outputFormats);
    }

    public void registerQuery(Query query)
    {
        queries.put(query.name(), query);
    }

    /// Adds an output format at runtime, like an extension that is loaded.
    public void addOutputFormat(OutputFormat outputFormat)
    {
        outputFormats.add(outputFormat);
    }

    /// Removes an output format at runtime, like an extension that is unloaded.
    public void removeOutputFormat(OutputFormat outputFormat)
    {
        outputFormats.remove(outputFormat);
    }

    /// Simulates that no output format supports the query results, so that formatting falls back
    /// to Markdown.
    public void setFallBackToMarkdown(boolean fallBackToMarkdown)
    {
        this.fallBackToMarkdown = fallBackToMarkdown;
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
    public FormattedResult formatResult(QueryResult result, OutputFormat format)
    {
        if (fallBackToMarkdown || !outputFormats.contains(format))
        {
            return new FormattedResult(MARKDOWN, "formatted:" + MARKDOWN.name());
        }
        return new FormattedResult(format, "formatted:" + format.name());
    }
}

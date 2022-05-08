package nl.ulso.macu.curator;

import nl.ulso.macu.query.*;
import nl.ulso.macu.vault.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Base class for {@link Curator}s on top of a {@link FileSystemVault} and custom, built-in
 * queries.
 * <p/>
 * Whenever a change in the underlying vault is detected, all queries are collected from all
 * documents in the vault. These queries are then executed and compared to the existing query
 * results within the documents. If a query result has changed, the document is rewritten to disk,
 * with the old query result replaced.
 * <p/>
 * There's plenty of ways to make this implementation more efficient, like caching the available
 * queries, running queries in parallel, writing documents in a non-blocking manner, and so on.
 * I'll dive into that when performance becomes an issue.
 */
public abstract class CuratorTemplate
        implements Curator, VaultChangedCallback
{
    private static final Logger LOGGER = getLogger(CuratorTemplate.class);

    private final FileSystemVault vault;
    private final QueryCatalog queryCatalog;

    public CuratorTemplate()
    {
        try
        {
            this.vault = createVault();
        }
        catch (IOException e)
        {
            LOGGER.error("Couldn't create vault. Reason: {}", e.toString());
            throw new RuntimeException(e);
        }
        this.queryCatalog = new InMemoryQueryCatalog();
        queryCatalog.register(new ListQuery(vault));
        queryCatalog.register(new TableQuery(vault));
        queryCatalog.register(new TableOfContentsQuery());
        registerQueries(queryCatalog, vault);
    }

    protected final FileSystemVault createVaultForPathInUserHome(String... path)
            throws IOException
    {
        return new FileSystemVault(Path.of(java.lang.System.getProperty("user.home"), path));
    }

    protected abstract FileSystemVault createVault()
            throws IOException;

    protected abstract void registerQueries(QueryCatalog catalog, Vault vault);

    @Override
    public void runOnce()
    {
        LOGGER.info("Running this curator once");
        vaultChanged();
    }

    @Override
    public final void run()
            throws IOException, InterruptedException
    {
        vault.setVaultChangedCallback(this);
        vault.watchForChanges();
    }

    /**
     * This is a simple, non-optimized implementation of the simple callback: it collects all
     * queries in the vault, runs them all, and writes the documents that have changed back to disk.
     */
    @Override
    public void vaultChanged()
    {
        LOGGER.debug("Re-running all queries");
        runAllQueries().entrySet().stream()
                .sorted(comparingInt(e -> e.getKey().resultStartIndex()))
                .collect(groupingBy(e -> e.getKey().document()))
                .forEach(this::writeDocument);
        LOGGER.debug("Done!");
    }

    /**
     * Runs all queries in the vault and collects the queries whose outputs have changed compared
     * to what's in memory right now.
     */
    Map<QueryBlock, String> runAllQueries()
    {
        Map<QueryBlock, String> writeQueue = new HashMap<>();
        vault.findAllQueryBlocks().forEach(queryBlock -> {
            var query = queryCatalog.query(queryBlock.name());
            var result = query.run(queryBlock);
            var output = result.toMarkdown();
            if (!queryBlock.result().contentEquals(output))
            {
                LOGGER.debug("Document '{}' query result change detected", queryBlock.document());
                writeQueue.put(queryBlock, output);
            }
        });
        return writeQueue;
    }

    void writeDocument(Document document, List<Map.Entry<QueryBlock, String>> outputs)
    {
        LOGGER.info("Rewriting document '{}'", document);
        var path = vault.resolveAbsolutePath(document);
        try (var writer = Files.newBufferedWriter(path); var out = new PrintWriter(writer))
        {
            int index = 0;
            for (Map.Entry<QueryBlock, String> entry : outputs)
            {
                var queryBlock = entry.getKey();
                printDocumentLines(out, document, index, queryBlock.resultStartIndex());
                out.println(entry.getValue());
                index = queryBlock.resultEndIndex();
            }
            printDocumentLines(out, document, index, -1);
        }
        catch (IOException e)
        {
            LOGGER.warn("Couldn't write file: {}", document.name());
        }
    }

    private void printDocumentLines(PrintWriter out, Document document, int start, int end)
    {
        var lines = document.lines();
        if (end == -1)
        {
            end = lines.size();
        }
        for (int i = start; i < end; i++)
        {
            out.println(lines.get(i));
        }
    }

    public final FileSystemVault vault()
    {
        return vault;
    }

    public final QueryCatalog queryCatalog()
    {
        return queryCatalog;
    }
}

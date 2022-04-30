package nl.ulso.macu.system;

import nl.ulso.macu.query.InMemoryQueryCatalog;
import nl.ulso.macu.query.QueryCatalog;
import nl.ulso.macu.vault.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Base class for {@link System}s on top of a {@link FileSystemVault} and custom, built-in
 * queries.
 * <p/>
 * Whenever a change in the underlying vault is detected, all queries are collected from all
 * documents in the vault. These queries are then executed and compared to the existing query
 * results withinthe documents. If a query result has changed, the document is rewritten to disk,
 * with the old query result replaced.
 * <p/>
 * There's plenty of ways to make this implementation more efficient, like caching the available
 * queries, running queries in parallel, writing documents in a non-blocking manner, and so on.
 *
 */
public abstract class SystemTemplate
        implements System, VaultChangedCallback
{
    private static final Logger LOGGER = getLogger(SystemTemplate.class);

    private final FileSystemVault vault;
    private final QueryCatalog queryCatalog;

    public SystemTemplate()
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
    public final void run()
            throws IOException, InterruptedException
    {
        vault.setVaultChangedCallback(this);
        vault.watchForChanges();
    }

    /**
     * This is a simple, non-optimized implementation of the simple callback: it collects all
     * queries in the vault, runs them all, and writes the ones that have changed back to disk.
     */
    @Override
    public void vaultChanged()
    {
        LOGGER.debug("Vault changed! Re-running all queries");
        runAllQueries().entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().resultStartIndex()))
                .collect(Collectors.groupingBy(e -> e.getKey().document()))
                .forEach(this::writeDocument);
        LOGGER.debug("Done!");
    }

    private Map<QueryBlock, String> runAllQueries()
    {
        Map<QueryBlock, String> writeQueue = new HashMap<>();
        vault.findAllQueryBlocks().forEach(queryBlock -> {
            var query = queryCatalog.query(queryBlock.name());
            var result = query.run(queryBlock);
            LOGGER.debug("Document '{}', query '{}', success: {}",
                    queryBlock.document(), query.name(), result.isSuccess());
            var output = result.toString();
            if (!queryBlock.result().contentEquals(output))
            {
                LOGGER.debug("Document '{}' query result change detected", queryBlock.document());
                writeQueue.put(queryBlock, output);
            }
        });
        return writeQueue;
    }

    private void writeDocument(Document document, List<Map.Entry<QueryBlock, String>> outputs)
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

    public final Vault vault()
    {
        return vault;
    }

    public final QueryCatalog queryCatalog()
    {
        return queryCatalog;
    }
}

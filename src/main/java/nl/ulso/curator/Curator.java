package nl.ulso.curator;

import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Vault;

import java.util.Set;

public interface Curator
{
    /// Run the curator in the specified mode. As a daemon: watch for changes, continuously running
    /// queries, and updating documents on disk when needed. As a one-off: run all queries in all
    /// documents, write all updated documents back to disk, then exit.
    ///
    /// Technically, there's no need to pass the run mode as an argument, as it can be pulled from
    /// the environment directory through [RunMode#get()]. It's good to be explicit, though.
    void run(RunMode runMode);

    Vault vault();

    Query query(String queryName);

    Set<OutputFormat> outputFormats();

    boolean supports(QueryResult queryResult, OutputFormat outputFormat);

    String format(QueryResult result, OutputFormat format);
}

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

    /// @return all output formats currently available in this curator: the built-in ones and those
    /// introduced by the active extensions. This set changes as extensions are loaded and unloaded.
    Set<OutputFormat> outputFormats();

    /// Formats a query result in the requested output format. If the format has no formatter for
    /// the type of query result, the result is formatted as [OutputFormat#MARKDOWN] instead; the
    /// returned [FormattedResult] tells which format the output is actually in.
    FormattedResult formatResult(QueryResult result, OutputFormat format);
}

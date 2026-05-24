package nl.ulso.curator;

public interface Curator
{
    /// Run the curator in the specified mode. As a daemon: watch for changes, continuously running
    /// queries, and updating documents on disk when needed. As a one-off: run all queries in all
    /// documents, write all updated documents back to disk, then exit.
    ///
    /// Technically, there's no need to pass the run mode as an argument, as it can be pulled from
    /// the environment directory through [RunMode#get()]. It's good to be explicit, though.
    void run(RunMode runMode);
}

package nl.ulso.curator;

public interface Curator
{
    /// Run the curator in the specified mode. As a daemon: watch for changes, continuously running
    /// queries and updating documents on disk when needed. As a one-off: run all queries in all
    /// documents, write all updated documents back to disk, then exit.
    void run(RunMode runMode);
}

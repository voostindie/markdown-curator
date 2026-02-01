package nl.ulso.curator;

public interface Curator
{
    /// Run the curator once, running _all_ queries in _all_ documents and writing _all_ updated
    /// documents back to disk, then exit.
    void runOnce();

    /// Start the curator and watch for changes, continuously running queries and updating
    /// documents on disk when needed.
    void run();
}

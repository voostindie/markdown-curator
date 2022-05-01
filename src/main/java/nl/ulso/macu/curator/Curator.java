package nl.ulso.macu.curator;

import java.io.IOException;

/**
 * Defines a single configuration of a vault, a query catalog and anything else the curator might
 * encapsulate.
 */
public interface Curator
{
    /**
     * Runs this curator once. Changes to the underlying vault are <b>not</b> monitored!
     */
    void runOnce()
        throws IOException;

    /**
     * Runs this curator by watching the vault for changes and "doing stuff"" as a result of
     * these changes. This method blocks.
     */
    void run()
            throws IOException, InterruptedException;
}

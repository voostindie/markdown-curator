package nl.ulso.curator.statistics;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.PrintWriter;

/// Collects statistics and logs them.
///
/// Statistics are collected right before logging happens, so they are always fresh.
public interface Statistics
{
    /// Collects fresh statistics and writes them to the logger at the specified level.
    void logTo(Logger logger, Level level);

    /// Collects fresh statistics and writes them to the provided writer.
    void logTo(PrintWriter writer);
}

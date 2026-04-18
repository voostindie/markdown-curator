package nl.ulso.curator.statistics;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

import java.io.PrintWriter;
import java.util.*;

@Singleton
final class DefaultStatistics
    implements Statistics
{
    private final Set<MeasurementTracker> trackers;

    @Inject
    public DefaultStatistics(Set<MeasurementTracker> trackers)
    {
        this.trackers = trackers;
    }

    @Override
    public void logTo(Logger logger, Level level)
    {
        if (!logger.isEnabledForLevel(level))
        {
            return;
        }
        logTo(new LoggerWriter(logger.atLevel(level)));
    }

    @Override
    public void logTo(PrintWriter writer)
    {
        var collector = new DefaultMeasurementCollector();
        trackers.forEach(tracker -> tracker.collectMeasurements(collector));
        writeStatistics(collector.measurements(), writer);
    }

    private void writeStatistics(Map<String, Map<String, Long>> measurements, PrintWriter writer)
    {
        var modules = new ArrayList<>(measurements.keySet());
        Collections.sort(modules);
        modules.forEach(module ->
        {
            writer.println(String.format("%s:", module));
            var totals = measurements.get(module);
            var entities = new ArrayList<>(totals.keySet());
            Collections.sort(entities);
            entities.forEach(entity ->
            {
                var total = totals.get(entity);
                writer.println(String.format("  %s: %d", entity, total));
            });
        });
    }

    /// Wraps a [LoggingEventBuilder] and writes [String]s to it. The only method implemented is
    /// [println], because that's the only method called by
    /// [DefaultStatistics#writeStatistics(Map, PrintWriter)].
    private static class LoggerWriter
        extends PrintWriter
    {
        private final LoggingEventBuilder builder;

        public LoggerWriter(LoggingEventBuilder builder)
        {
            super(nullWriter());
            this.builder = builder;
        }

        @Override
        public void println(String string)
        {
            builder.log("> " + string);
        }
    }
}

package nl.ulso.curator.statistics;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultStatisticsTest
{
    @Test
    void testLog()
    {
        var tracker = new TrackerStub();
        var statistics = new DefaultStatistics(Set.of(tracker));
        var writer = new StringWriter();
        statistics.logTo(new PrintWriter(writer, true));
        assertThat(writer.toString()).isEqualTo("""
            lang:
              string: 50
            module1:
              entity1: 10
              entity2: 20
            module2:
              entity3: 30
              entity4: 40
            """);
    }

    private static class TrackerStub
        implements MeasurementTracker
    {
        @Override
        public void collectMeasurements(MeasurementCollector collector)
        {
            collector
                .forModule("module1")
                .total("entity1", 10)
                .total("entity2", 20)
                .total(String.class, 50)
                .forModule("module2")
                .total("entity3", 30)
                .total("entity4", 40);
        }
    }
}

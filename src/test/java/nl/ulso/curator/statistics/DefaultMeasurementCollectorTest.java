package nl.ulso.curator.statistics;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class DefaultMeasurementCollectorTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void testTotalMeasurement()
    {
        var collector = new DefaultMeasurementCollector();
        collector.total("module", "entity", 10);
        assertThat(collector.totalFor("module", "entity")).isEqualTo(10);
    }

    @Test
    void testModuleMeasurements()
    {
        var collector = new DefaultMeasurementCollector();
        collector.forModule("module1")
            .total("entity1", 10)
            .forModule("module2")
            .total("entity2", 15)
            .forModule("module1")
            .total("entity3", 20)
            .total("module2", "entity4", 25);
        softly.assertThat(collector.totalFor("module1", "entity1")).isEqualTo(10);
        softly.assertThat(collector.totalFor("module1", "entity3")).isEqualTo(20);
        softly.assertThat(collector.totalFor("module2", "entity2")).isEqualTo(15);
        softly.assertThat(collector.totalFor("module2", "entity4")).isEqualTo(25);
    }
}

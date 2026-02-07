package nl.ulso.curator.addon.project;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeightedValueTest
{
    @Test
    void sameValueAndWeightsAreEqual()
    {
        var value1 = new WeightedValue(42, 0);
        var value2 = new WeightedValue(42, 0);
        assertThat(value1).isEqualTo(value2);
    }

    @Test
    void differentValuesSameWeightsAreEqual()
    {
        var value1 = new WeightedValue(24, 0);
        var value2 = new WeightedValue(42, 0);
        assertThat(value1).isEqualTo(value2);
    }

    @Test
    void differentWeightAreInequal()
    {
        var value1 = new WeightedValue(24, 1);
        var value2 = new WeightedValue(24, 0);
        assertThat(value1).isNotEqualTo(value2);
    }

    @Test
    void higherWeightHasPrecedence()
    {
        var value1 = new WeightedValue(24, 100);
        var value2 = new WeightedValue(42, 0);
        assertThat(value1).isGreaterThan(value2);
    }
}

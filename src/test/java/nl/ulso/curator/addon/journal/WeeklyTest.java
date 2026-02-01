package nl.ulso.curator.addon.journal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyTest
{
    @Test
    void toStringMatchesPattern()
    {
        var weekly = new Weekly(2023, 8);
        assertThat(weekly.toString()).matches(Weekly.DOCUMENT_NAME_PATTERN);
    }

    @Test
    void compareEqual()
    {
        var w1 = new Weekly(2023, 8);
        var w2 = new Weekly(2023, 8);
        assertThat(w1).isEqualTo(w2);
    }

    @Test
    void compareOnYear()
    {
        var w1 = new Weekly(2022, 8);
        var w2 = new Weekly(2023, 8);
        assertThat(w1).isLessThan(w2);
    }

    @Test
    void compareOnWeek()
    {
        var w1 = new Weekly(2022, 8);
        var w2 = new Weekly(2023, 9);
        assertThat(w1).isLessThan(w2);
    }
}

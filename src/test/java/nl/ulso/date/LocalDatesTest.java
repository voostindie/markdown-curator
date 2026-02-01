package nl.ulso.date;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDatesTest
{
    @Test
    void validDate()
    {
        assertThat(LocalDates.parseDateOrNull("1976-11-30")).isNotNull();
    }

    @Test
    void invalidDateBecomesNull()
    {
        assertThat(LocalDates.parseDateOrNull("NO DATE")).isNull();
    }

    @Test
    void invalidDateBecomesUsesSupplier()
    {
        var defaultDate = LocalDate.now();
        assertThat(LocalDates.parseDate("NO DATE", () -> defaultDate)).isSameAs(defaultDate);
    }
}

package nl.ulso.curator.addon.newday;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static nl.ulso.curator.addon.newday.NewDay.NEW_DAY;
import static org.assertj.core.api.Assertions.assertThat;

class NewDayTest
{
    @Test
    void equalsAndHashCode()
    {
        EqualsVerifier.forClass(NewDay.class).verify();
    }

    @Test
    void newDayToString()
    {
        var newDay = new NewDay();
        assertThat(newDay).hasToString("⏰");
    }

    @Test
    void newDayChangeConstant()
    {
        assertThat(NEW_DAY).isNotNull();
        assertThat(NEW_DAY.payloadType()).isEqualTo(NewDay.class);
        assertThat(NEW_DAY.value()).isInstanceOf(NewDay.class);
    }
}

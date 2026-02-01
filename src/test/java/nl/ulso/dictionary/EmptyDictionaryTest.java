package nl.ulso.dictionary;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static org.assertj.core.api.Assertions.assertThat;

class EmptyDictionaryTest
{
    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(EmptyDictionary.class).verify();
    }

    @Test
    void isAlwaysEmpty()
    {
        assertThat(emptyDictionary().isEmpty()).isTrue();
    }

    @Test
    void stringReturnsDefault()
    {
        assertThat(emptyDictionary().string("foo", "bar")).isEqualTo("bar");
    }

    @Test
    void integerReturnsDefault()
    {
        assertThat(emptyDictionary().integer("foo", 42)).isEqualTo(42);
    }

    @Test
    void dateReturnsDefault()
    {
        var date = LocalDate.now();
        assertThat(emptyDictionary().date("foo", date)).isEqualTo(date);
    }

    @Test
    void listOfStringsIsEmpty()
    {
        assertThat(emptyDictionary().listOfStrings("foo")).isEmpty();
    }

    @Test
    void listOfIntegersIsEmpty()
    {
        assertThat(emptyDictionary().listOfIntegers("foo")).isEmpty();
    }

    @Test
    void listOfDatesIsEmpty()
    {
        assertThat(emptyDictionary().listOfDates("foo")).isEmpty();
    }
}
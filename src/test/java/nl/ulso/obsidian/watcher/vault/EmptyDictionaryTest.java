package nl.ulso.obsidian.watcher.vault;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static nl.ulso.obsidian.watcher.vault.Dictionary.emptyDictionary;
import static org.assertj.core.api.Assertions.assertThat;

class EmptyDictionaryTest
{

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
        var date = new Date();
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
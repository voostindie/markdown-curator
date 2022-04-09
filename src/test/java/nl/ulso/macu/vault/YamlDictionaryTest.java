package nl.ulso.macu.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static nl.ulso.macu.vault.Dictionary.yamlDictionary;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class YamlDictionaryTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(YamlDictionary.class).withIgnoredFields("dateCache").verify();
    }

    @Test
    void emptyList()
    {
        var dictionary = yamlDictionary(Collections.emptyList());
        assertThat(dictionary.isEmpty()).isTrue();
    }

    @Test
    void fullFrontMatter()
    {
        var dictionary = dictionary("""
                ---
                foo: bar
                answer: 42
                ---
                """);
        softly.assertThat(dictionary.isEmpty()).isFalse();
        softly.assertThat(dictionary.string("foo", null)).isEqualTo("bar");
        softly.assertThat(dictionary.integer("answer", -1)).isEqualTo(42);
    }

    @Test
    void emptyFrontMatter()
    {
        var dictionary = dictionary("""
                ---
                ---
                """);
        assertThat(dictionary.isEmpty()).isTrue();
    }

    @Test
    void singleYamlMap()
    {
        var dictionary = dictionary("""
                foo: bar
                answer: 42
                """);
        softly.assertThat(dictionary.isEmpty()).isFalse();
        softly.assertThat(dictionary.string("foo", null)).isEqualTo("bar");
        softly.assertThat(dictionary.integer("answer", -1)).isEqualTo(42);
    }

    @Test
    void invalidYaml()
    {
        var dictionary = dictionary("foo");
        assertThat(dictionary.isEmpty()).isTrue();
    }

    @Test
    void validIntegerValue()
    {
        var dictionary = dictionary("""
                foo: 42
                """);
        assertThat(dictionary.integer("foo", -1)).isEqualTo(42);
    }

    @Test
    void invalidIntegerValue()
    {
        var dictionary = dictionary("""
                foo: bar
                """);
        assertThat(dictionary.integer("foo", -1)).isEqualTo(-1);
    }

    @Test
    void validDate()
    {
        var dictionary = dictionary("date: 1976-11-30");
        assertThat(dictionary.date("date", null).toString())
                .isEqualTo("Tue Nov 30 00:00:00 CET 1976");
    }

    @Test
    void singleValueIntegerValue()
    {
        var dictionary = dictionary("""
                foo: [42, 84]
                """);
        assertThat(dictionary.integer("foo", -1)).isEqualTo(42);
    }

    @Test
    void integerListFromSingleValue()
    {
        var dictionary = dictionary("""
                foo: 42
                """);
        List<Integer> list = dictionary.listOfIntegers("foo");
        softly.assertThat(list.size()).isEqualTo(1);
        softly.assertThat(list).first().isEqualTo(42);
    }

    @Test
    void stringListMissingValue()
    {
        var dictionary = dictionary("""
                foo: 42
                """);
        List<Integer> list = dictionary.listOfIntegers("bar");
        assertThat(list).isEmpty();
    }

    @Test
    void dateListMultipleValues()
    {
        var dictionary = dictionary("""
                dates: [1976-11-30, 1977-11-11, 2003-05-08]
                """);
        List<Date> list = dictionary.listOfDates("dates");
        assertThat(list.size()).isEqualTo(3);

    }

    private Dictionary dictionary(String yaml)
    {
        return yamlDictionary(yaml.lines().collect(toList()));
    }
}
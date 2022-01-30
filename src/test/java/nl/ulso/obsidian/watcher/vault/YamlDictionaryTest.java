package nl.ulso.obsidian.watcher.vault;

import org.junit.jupiter.api.Test;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static nl.ulso.obsidian.watcher.vault.Dictionary.yamlDictionary;
import static org.assertj.core.api.Assertions.assertThat;

class YamlDictionaryTest
{
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
        assertThat(dictionary.isEmpty()).isFalse();
        assertThat(dictionary.string("foo", null)).isEqualTo("bar");
        assertThat(dictionary.integer("answer", -1)).isEqualTo(42);
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
        assertThat(dictionary.isEmpty()).isFalse();
        assertThat(dictionary.string("foo", null)).isEqualTo("bar");
        assertThat(dictionary.integer("answer", -1)).isEqualTo(42);
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
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0)).isEqualTo(42);
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
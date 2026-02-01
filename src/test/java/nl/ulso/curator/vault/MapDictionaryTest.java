package nl.ulso.curator.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class MapDictionaryTest
{
    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(MapDictionary.class).verify();
    }

    @Test
    void emptyMapIsEmptyDictionary()
    {
        assertThat(new MapDictionary(emptyMap()).isEmpty()).isTrue();
    }

    @Test
    void defaultIfNoValueExists()
    {
        var dictionary = new MapDictionary(emptyMap());
        assertThat(dictionary.integer("foo", -1)).isEqualTo(-1);
    }

    @Test
    void valueIfItExists()
    {
        var dictionary = new MapDictionary(Map.of("foo", 42));
        assertThat(dictionary.integer("foo", -1)).isEqualTo(42);
    }

    @Test
    void scalarBecomesList()
    {
        var dictionary = new MapDictionary(Map.of("foo", 42));
        var value = dictionary.listOfIntegers("foo");
        assertThat(value)
                .hasSize(1)
                .first()
                .isEqualTo(42);
    }

    @Test
    void listBecomesScalar()
    {
        var dictionary = new MapDictionary(Map.of("strings", List.of("foo", "bar")));
        var value = dictionary.string("strings", "baz");
        assertThat(value).isEqualTo("foo");
    }

    @Test
    void scalarFromEmptyListReturnsDefault()
    {
        var dictionary = new MapDictionary(Map.of("list", emptyList()));
        var value = dictionary.string("list", "foo");
        assertThat(value).isEqualTo("foo");
    }

    @Test
    void listFromEmptyListReturnsEmptyList()
    {
        var dictionary = new MapDictionary(Map.of("list", emptyList()));
        var value = dictionary.listOfStrings("list");
        assertThat(value).isEmpty();
    }

    @Test
    void datePropertyPresent()
    {
        var dictionary = new MapDictionary(Map.of("date", "?"));
        var value = dictionary.date("date", null);
        assertThat(value).isNull();
        assertThat(dictionary.hasProperty("date")).isTrue();
    }
}

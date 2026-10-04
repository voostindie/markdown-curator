package nl.ulso.curator.server;

import org.eclipse.jetty.util.Fields;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JettyFieldsDictionaryTest
{
    @Test
    void propertyNames()
    {
        var fields = new Fields();
        fields.put("title", "My Title");
        fields.put("count", "10");
        var dictionary = new JettyFieldsDictionary(fields);

        assertThat(dictionary.propertyNames()).containsExactlyInAnyOrder("title", "count");
        assertThat(new JettyFieldsDictionary(new Fields()).propertyNames()).isEmpty();
    }

    @Test
    void isEmpty()
    {
        var emptyDictionary = new JettyFieldsDictionary(new Fields());
        assertThat(emptyDictionary.isEmpty()).isTrue();

        var fields = new Fields();
        fields.put("key", "value");
        var populatedDictionary = new JettyFieldsDictionary(fields);
        assertThat(populatedDictionary.isEmpty()).isFalse();
    }

    @Test
    void hasProperty()
    {
        var fields = new Fields();
        fields.put("title", "My Title");
        var dictionary = new JettyFieldsDictionary(fields);

        assertThat(dictionary.hasProperty("title")).isTrue();
        assertThat(dictionary.hasProperty("missing")).isFalse();
    }

    @Test
    void getProperty()
    {
        var fields = new Fields();
        fields.put("title", "My Title");
        var dictionary = new JettyFieldsDictionary(fields);

        assertThat(dictionary.getProperty("title")).contains(List.of("My Title"));
        assertThat(dictionary.getProperty("missing")).contains(List.of());
    }

    @Test
    void string()
    {
        var fields = new Fields();
        fields.put("title", "My Title");
        var dictionary = new JettyFieldsDictionary(fields);

        assertThat(dictionary.string("title", "default")).isEqualTo("My Title");
        assertThat(dictionary.string("missing", "default")).isEqualTo("default");
    }

    @Test
    void integer()
    {
        var fields = new Fields();
        fields.put("count", "10");
        fields.put("invalidCount", "not-a-number");
        var dictionary = new JettyFieldsDictionary(fields);

        assertThat(dictionary.integer("count", 0)).isEqualTo(10);
        assertThat(dictionary.integer("invalidCount", 99)).isEqualTo(99);
        assertThat(dictionary.integer("missing", 99)).isEqualTo(99);
    }

    @Test
    void date()
    {
        var fields = new Fields();
        fields.put("published", "2026-10-04");
        fields.put("invalidDate", "bad-date");
        var dictionary = new JettyFieldsDictionary(fields);

        assertThat(dictionary.date("published", LocalDate.EPOCH))
            .isEqualTo(LocalDate.of(2026, 10, 4));
        assertThat(dictionary.date("invalidDate", LocalDate.EPOCH))
            .isEqualTo(LocalDate.EPOCH);
        assertThat(dictionary.date("missing", LocalDate.EPOCH))
            .isEqualTo(LocalDate.EPOCH);
    }

    @Test
    void bool()
    {
        var fields = new Fields();
        fields.put("active", "true");
        fields.put("inactive", "false");
        var dictionary = new JettyFieldsDictionary(fields);

        assertThat(dictionary.bool("active", false)).isTrue();
        assertThat(dictionary.bool("inactive", true)).isFalse();
        assertThat(dictionary.bool("missing", true)).isTrue();
        assertThat(dictionary.bool("missing", false)).isFalse();
    }

    @Test
    void listOfStrings()
    {
        var fields = new Fields();
        var tagsField = new Fields.Field("tags", List.of("java", "curator", "testing"));
        fields.put(tagsField);
        var dictionary = new JettyFieldsDictionary(fields);

        assertThat(dictionary.listOfStrings("tags"))
            .containsExactly("java", "curator", "testing");
        assertThat(dictionary.listOfStrings("missing")).isEmpty();
    }

    @Test
    void listOfIntegers()
    {
        var fields = new Fields();
        var numbersField = new Fields.Field("numbers", List.of("1", "invalid", "2", "3"));
        fields.put(numbersField);
        var dictionary = new JettyFieldsDictionary(fields);

        assertThat(dictionary.listOfIntegers("numbers"))
            .containsExactly(1, 2, 3);
        assertThat(dictionary.listOfIntegers("missing")).isEmpty();
    }

    @Test
    void listOfDates()
    {
        var fields = new Fields();
        var datesField = new Fields.Field("dates", List.of("2026-01-01", "invalid", "2026-12-31"));
        fields.put(datesField);
        var dictionary = new JettyFieldsDictionary(fields);

        assertThat(dictionary.listOfDates("dates"))
            .containsExactly(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
            );
        assertThat(dictionary.listOfDates("missing")).isEmpty();
    }

    @Test
    void toYamlStringThrowsUnsupportedOperationException()
    {
        var dictionary = new JettyFieldsDictionary(new Fields());
        assertThatThrownBy(dictionary::toYamlString)
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("Conversion of JettyFields to YAML is not supported.");
    }
}

package nl.ulso.markdown_curator.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static nl.ulso.markdown_curator.vault.Document.newDocument;
import static org.assertj.core.api.Assertions.assertThat;

class FrontMatterTest
{
    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(FrontMatter.class)
                .withPrefabValues(Document.class,
                        newDocument("1", 0, Collections.emptyList()),
                        newDocument("2", 0, Collections.emptyList()))
                .withIgnoredFields("document")
                .verify();
    }

    @Test
    void emptyInput()
    {
        var frontMatter = new FrontMatter(emptyList());
        assertThat(frontMatter.isEmpty()).isTrue();
    }

    @Test
    void validInput()
    {
        assertThat(frontMatter().isEmpty()).isFalse();
    }

    @Test
    void dictionaryString()
    {
        assertThat(frontMatter().string("bar", null)).isEqualTo("baz");
    }

    @Test
    void dictionaryStringList()
    {
        var aliases = frontMatter().listOfStrings("aliases");
        assertThat(aliases).containsExactly("one", "two", "three");
    }

    @Test
    void dictionaryInteger()
    {
        assertThat(frontMatter().integer("foo", -1)).isEqualTo(42);
    }

    @Test
    void dictionaryDate()
    {
        assertThat(frontMatter().date("date", null)).isNotNull();
    }

    @Test
    void dictionaryIntegerList()
    {
        assertThat(frontMatter().listOfIntegers("foo")).hasSize(1);
    }

    @Test
    void dictionaryDateList()
    {
        assertThat(frontMatter().listOfDates("date")).hasSize(1);
    }

    private FrontMatter frontMatter()
    {
        return new FrontMatter(List.of(
                "foo: 42",
                "bar: baz",
                "date: 1976-11-30",
                "aliases: [one, two, three]"
        ));
    }

}
package nl.ulso.markdown_curator.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static nl.ulso.markdown_curator.vault.Document.newDocument;

@ExtendWith(SoftAssertionsExtension.class)
class TextBlockTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(TextBlock.class)
                .withPrefabValues(Document.class,
                        newDocument("1", 0, Collections.emptyList()),
                        newDocument("2", 0, Collections.emptyList()))
                .withIgnoredFields("document")
                .verify();
    }

    @Test
    void empty()
    {
        TextBlock empty = new TextBlock(emptyList());
        softly.assertThat(empty.isEmpty()).isTrue();
        softly.assertThat(empty.lines()).isEmpty();
        softly.assertThat(empty.content()).isEmpty();
    }

    @Test
    void single()
    {
        var single = new TextBlock(List.of("foo bar"));
        softly.assertThat(single.isEmpty()).isFalse();
        softly.assertThat(single.lines().size()).isEqualTo(1);
        softly.assertThat(single.content()).isEqualTo("foo bar");
    }

    @Test
    void trimmedFullText()
    {
        var text = new TextBlock(List.of("", "foo", "bar", ""));
        softly.assertThat(text.isEmpty()).isFalse();
        softly.assertThat(text.lines()).containsExactly("foo", "bar");
        softly.assertThat(text.content()).isEqualTo("foo\nbar");
    }
}
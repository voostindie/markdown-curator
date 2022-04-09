package nl.ulso.macu.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class TextTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(Text.class).verify();
    }

    @Test
    void empty()
    {
        Text empty = new Text(emptyList());
        softly.assertThat(empty.isEmpty()).isTrue();
        softly.assertThat(empty.lines()).isEmpty();
        softly.assertThat(empty.content()).isEmpty();
    }

    @Test
    void single()
    {
        var single = new Text(List.of("foo bar"));
        softly.assertThat(single.isEmpty()).isFalse();
        softly.assertThat(single.lines().size()).isEqualTo(1);
        softly.assertThat(single.content()).isEqualTo("foo bar");
    }

    @Test
    void trimmedFullText()
    {
        var text = new Text(List.of("", "foo", "bar", ""));
        softly.assertThat(text.isEmpty()).isFalse();
        softly.assertThat(text.lines()).containsExactly("foo", "bar");
        softly.assertThat(text.content()).isEqualTo("foo\nbar");
    }
}
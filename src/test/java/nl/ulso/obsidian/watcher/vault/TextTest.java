package nl.ulso.obsidian.watcher.vault;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class TextTest
{
    @Test
    void emptyText()
    {
        var text = new Text(emptyList());
        assertThat(text.isEmpty()).isTrue();
        assertThat(text.lines()).isEmpty();
        assertThat(text.content()).isEqualTo("");
    }

    @Test
    void singleLineText()
    {
        var text = new Text(List.of("foo bar"));
        assertThat(text.isEmpty()).isFalse();
        assertThat(text.lines().size()).isEqualTo(1);
        assertThat(text.content()).isEqualTo("foo bar");
    }

    @Test
    void trimmedFullText()
    {
        var text = new Text(List.of("", "foo", "bar", ""));
        assertThat(text.isEmpty()).isFalse();
        assertThat(text.lines().size()).isEqualTo(2);
        assertThat(text.lines().get(0)).isEqualTo("foo");
        assertThat(text.lines().get(1)).isEqualTo("bar");
        assertThat(text.content()).isEqualTo("foo\nbar");
    }

    @Test
    void visitor()
    {
        var text = new Text(emptyList());
        ElementCounter counter = new ElementCounter();
        text.accept(counter);
        assertThat(counter.texts).isEqualTo(1);
    }
}
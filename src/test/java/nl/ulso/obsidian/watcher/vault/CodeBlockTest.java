package nl.ulso.obsidian.watcher.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(SoftAssertionsExtension.class)
class CodeBlockTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(CodeBlock.class).verify();
    }

    @Test
    void empty()
    {
        CodeBlock markersOnly = new CodeBlock(List.of("```", "```"));
        softly.assertThat(markersOnly.isEmpty()).isFalse();
        softly.assertThat(markersOnly.code()).isBlank();
        softly.assertThat(markersOnly.language()).isBlank();
    }

    @Test
    void singleNoLanguage()
    {
        var single = new CodeBlock(List.of("```", "foo bar", "```"));
        softly.assertThat(single.code()).isEqualTo("foo bar");
        softly.assertThat(single.language()).isBlank();
    }

    @Test
    void singleWithLanguage()
    {
        var single = new CodeBlock(List.of("```foo", "bar", "```"));
        softly.assertThat(single.code()).isEqualTo("bar");
        softly.assertThat(single.language()).isEqualTo("foo");
        softly.assertThat(single.content()).isEqualTo("```foo\nbar\n```");
    }
}
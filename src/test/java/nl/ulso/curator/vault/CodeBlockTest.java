package nl.ulso.curator.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static nl.ulso.curator.vault.Document.newDocument;

@ExtendWith(SoftAssertionsExtension.class)
class CodeBlockTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(CodeBlock.class)
                .withPrefabValues(Document.class,
                        newDocument("1", 0, Collections.emptyList()),
                        newDocument("2", 0, Collections.emptyList()))
                .withPrefabValues(Section.class,
                        new Section(1, "1", emptyList()),
                        new Section(1, "2", emptyList()))
                .withIgnoredFields("document", "section")
                .verify();
    }

    @Test
    void empty()
    {
        CodeBlock markersOnly = new CodeBlock(List.of("```", "```"));
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
        softly.assertThat(single.toMarkdown()).isEqualTo("```foo\nbar\n```\n");
    }
}
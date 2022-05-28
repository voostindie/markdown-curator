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
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class SectionTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(Section.class)
                .withPrefabValues(Document.class,
                        newDocument("1", 0, Collections.emptyList()),
                        newDocument("2", 0, Collections.emptyList()))
                .withPrefabValues(Section.class,
                        new Section(1, "1", emptyList(), emptyList()),
                        new Section(1, "2", emptyList(), emptyList()))
                .withIgnoredFields("document", "section", "anchor")
                .verify();
    }

    @Test
    void normalSection()
    {
        var section = new Section(
                42,
                "Section title",
                List.of(
                        "## Section title",
                        "",
                        "Lorem ipsum"),
                List.of(
                        new TextBlock(List.of("", "Lorem ipsum"))
                ));
        softly.assertThat(section.level()).isEqualTo(42);
        softly.assertThat(section.title()).isEqualTo("Section title");
        softly.assertThat(section.anchor()).isEqualTo("Section title");
        softly.assertThat(section.lines()).containsExactly("## Section title", "", "Lorem ipsum");
        softly.assertThat(section.fragments())
                .containsExactly(new TextBlock(List.of("", "Lorem ipsum")));
    }

    @Test
    void cleanedUpAnchor()
    {
        var section = new Section(0, "# {foo} > [bar] ;!@", emptyList(), emptyList());
        assertThat(section.anchor()).isEqualTo("foo bar");
    }

    @Test
    void emptySection()
    {
        var section = new Section(0, "", emptyList(), emptyList());
        softly.assertThat(section.lines()).isEmpty();
        softly.assertThat(section.fragments()).isEmpty();
    }
}
package nl.ulso.obsidian.watcher.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.ulso.obsidian.watcher.vault.Document.newDocument;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class DocumentTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(Document.class).withIgnoredFields("title").verify();
    }

    @Test
    void emptyDocument()
    {
        var document = newDocument("document", Collections.emptyList());
        softly.assertThat(document.title()).isEqualTo("document");
        softly.assertThat(document.frontMatter().isEmpty()).isTrue();
        softly.assertThat(document.lines()).isEmpty();
    }

    @Test
    void textOnlyDocument()
    {
        var document = newDocument("document", document("One-liner"));
        softly.assertThat(document.title()).isEqualTo("document");
        softly.assertThat(document.frontMatter().isEmpty()).isTrue();
        softly.assertThat(document.lines().get(0)).isEqualTo("One-liner");
    }

    @Test
    void frontMatterOnlyDocument()
    {
        var document = newDocument("document",
                List.of("---", "foo: bar", "---"));
        softly.assertThat(document.title()).isEqualTo("document");
        softly.assertThat(document.frontMatter().string("foo", null)).isEqualTo("bar");
        softly.assertThat(document.fragments().size()).isEqualTo(1);
    }

    @Test
    void titleOnlyDocument()
    {
        var document = newDocument("document", List.of("# Title"));
        assertThat(document.title()).isEqualTo("Title");
    }

    @Test
    void fullDocument()
    {
        var document = newDocument("document", document("""
                ---
                aliases: [alias]
                date: 1976-11-30
                priority: 100
                title: ignored
                ---
                # title
                        
                ## foo bar
                        
                lorem ipsum
                        
                        
                """));
        softly.assertThat(document.title()).isEqualTo("title");
        softly.assertThat(document.frontMatter().integer("priority", -1)).isEqualTo(100);
        softly.assertThat(document.frontMatter().date("date", null).toString())
                .isEqualTo("Tue Nov 30 00:00:00 CET 1976");
        softly.assertThat(document.fragment(1).content())
                .isEqualTo("# title\n\n## foo bar\n\nlorem ipsum");
    }

    @Test
    void multipleSameLevelSections()
    {
        var document = newDocument("document", document("""
                ## Section 1
                Introduction
                ## Section 2
                ### Subsection 1
                ### Subsection 2
                ## Section 3
                """));
        softly.assertThat(document.title()).isEqualTo("document");
        softly.assertThat(document.frontMatter().isEmpty()).isTrue();
        softly.assertThat(document.fragments().size()).isEqualTo(4);
        softly.assertThat(document.fragment(1)).isInstanceOf(Section.class);
        softly.assertThat(document.fragment(2)).isInstanceOf(Section.class);
        var section = (Section) document.fragment(2);
        softly.assertThat(section.fragments().size()).isEqualTo(2);
    }

    @Test
    void unclosedYamlFrontMatter()
    {
        String text = """
                ---
                foo: bar
                        
                # Title""";
        var document = newDocument("document", document(text));
        softly.assertThat(document.title()).isEqualTo("document");
        softly.assertThat(document.content()).isEqualTo(text);
    }

    @Test
    void invalidYamlFrontMatter()
    {
        String text = """
                ---
                42
                ---
                # Title""";
        var document = newDocument("document", document(text));
        softly.assertThat(document.frontMatter().isEmpty()).isTrue();
        softly.assertThat(document.title()).isEqualTo("Title");
        softly.assertThat(document.content()).isEqualTo("---\n42\n---\n# Title");
    }

    @Test
    void codeBlock()
    {
        String text = """
                line
                ```js
                let foo = 42;
                ```
                line
                """;
        var document = newDocument("document", document(text));
        softly.assertThat(document.fragments().size()).isEqualTo(4);
        softly.assertThat(document.fragment(2)).isInstanceOf(CodeBlock.class);
        var block = (CodeBlock) document.fragment(2);
        softly.assertThat(block.language()).isEqualTo("js");
        softly.assertThat(block.code()).isEqualTo("let foo = 42;");
    }

    @Test
    void emptyCodeBlock()
    {
        String text = """
                ```
                ```
                """;
        var document = newDocument("document", document(text));
        softly.assertThat(document.fragments().size()).isEqualTo(2);
        softly.assertThat(document.fragment(1)).isInstanceOf(CodeBlock.class);
        var block = (CodeBlock) document.fragment(1);
        softly.assertThat(block.language()).isBlank();
        softly.assertThat(block.code()).isBlank();
    }

    @Test
    void query()
    {
        String text = """
                <!--query-->
                foo
                <!--/query-->
                """;
        var document = newDocument("document", document(text));
        softly.assertThat(document.fragments().size()).isEqualTo(2);
        softly.assertThat(document.fragment(1)).isInstanceOf(Query.class);
        var query = (Query) document.fragment(1);
        softly.assertThat(query.configuration().isEmpty()).isTrue();
        softly.assertThat(query.result()).isEqualTo("foo");
    }

    private List<String> document(String text)
    {
        return text.lines().collect(toList());
    }
}
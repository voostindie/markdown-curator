package nl.ulso.markdown_curator.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static nl.ulso.markdown_curator.vault.Document.newDocument;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class DocumentTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(Document.class)
                .withPrefabValues(Document.class,
                        newDocument("1", 0, Collections.emptyList()),
                        newDocument("2", 0, Collections.emptyList()))
                .withPrefabValues(Section.class,
                        new Section(1, "1", emptyList(), emptyList()),
                        new Section(1, "2", emptyList(), emptyList()))
                .withIgnoredFields("document", "section", "title", "folder", "sortableTitle")
                .verify();
    }

    @Test
    void emptyDocument()
    {
        var document = newDocument("document", 0, Collections.emptyList());
        softly.assertThat(document.title()).isEqualTo("document");
        softly.assertThat(document.frontMatter().isEmpty()).isTrue();
        softly.assertThat(document.lines()).isEmpty();
    }

    @Test
    void textOnlyDocument()
    {
        var document = newDocument("document", 0, document("One-liner"));
        softly.assertThat(document.title()).isEqualTo("document");
        softly.assertThat(document.frontMatter().isEmpty()).isTrue();
        softly.assertThat(document.lines().get(0)).isEqualTo("One-liner");
    }

    @Test
    void frontMatterOnlyDocument()
    {
        var document = newDocument("document", 0,
                List.of("---", "foo: bar", "---"));
        softly.assertThat(document.title()).isEqualTo("document");
        softly.assertThat(document.frontMatter().string("foo", null)).isEqualTo("bar");
        softly.assertThat(document.fragments().size()).isEqualTo(1);
    }

    @Test
    void titleOnlyDocument()
    {
        var document = newDocument("document", 0, List.of("# Title"));
        assertThat(document.title()).isEqualTo("Title");
    }

    @Test
    void fullDocument()
    {
        var document = newDocument("document", 0, document("""
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
                .isEqualTo("1976-11-30");
        softly.assertThat(document.fragment(1).content())
                .isEqualTo("# title\n\n## foo bar\n\nlorem ipsum");
    }

    @Test
    void multipleSameLevelSections()
    {
        var document = newDocument("document", 0, document("""
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
        var document = newDocument("document", 0, document(text));
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
        var document = newDocument("document", 0, document(text));
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
        var document = newDocument("document", 0, document(text));
        softly.assertThat(document.fragments()).hasSize(4);
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
        var document = newDocument("document", 0, document(text));
        softly.assertThat(document.fragments()).hasSize(2);
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
        var document = newDocument("document", 0, document(text));
        softly.assertThat(document.fragments()).hasSize(2);
        softly.assertThat(document.fragment(1)).isInstanceOf(QueryBlock.class);
        var query = (QueryBlock) document.fragment(1);
        softly.assertThat(query.configuration().isEmpty()).isTrue();
        softly.assertThat(query.result()).isEqualTo("foo");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
                    ---
                    foo: bar
                    answer: 42
                    """,
            """
                    <!--query-->
                    foo
                    """,
            """
                    ```
                    code
                    """
    })
    void unclosedBlockBecomesTextBlock(String text)
    {
        var document = newDocument("document", 0, document(text));
        softly.assertThat(document.fragments()).hasSize(2);
        softly.assertThat(document.fragment(1)).isInstanceOf(TextBlock.class);
    }

    @Test
    void nestedQueryBlockBecomesTextBlockAndQueryBlock()
    {
        var document = newDocument("document", 0, document("""
                <!--query-->
                foo
                <!--query-->
                bar
                <!--/query-->
                """));
        softly.assertThat(document.fragments()).hasSize(3);
        softly.assertThat(document.fragment(1)).isInstanceOf(TextBlock.class);
        softly.assertThat(document.fragment(1).content()).isEqualTo("""
                <!--query-->
                foo
                """.trim());
        softly.assertThat(document.fragment(2)).isInstanceOf(QueryBlock.class);
        softly.assertThat(document.fragment(2).content()).isEqualTo("""
                <!--query-->
                bar
                <!--/query-->
                """.trim());
    }


    @Test
    void sortableTitle()
    {
        var document = newDocument("📄 Document 😱", 0, emptyList());
        assertThat(document.sortableTitle()).isEqualTo("Document");
    }

    private List<String> document(String text)
    {
        return text.lines().collect(toList());
    }
}
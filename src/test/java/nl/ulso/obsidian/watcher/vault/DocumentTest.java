package nl.ulso.obsidian.watcher.vault;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.ulso.obsidian.watcher.vault.Document.newDocument;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentTest
{
    @Test
    void emptyDocument()
    {
        var document = newDocument("document", Collections.emptyList());
        assertThat(document.title()).isEqualTo("document");
        assertThat(document.frontMatter().isEmpty()).isTrue();
        assertThat(document.lines()).isEmpty();
    }

    @Test
    void textOnlyDocument()
    {
        var document = newDocument("document", document("One-liner"));
        assertThat(document.title()).isEqualTo("document");
        assertThat(document.frontMatter().isEmpty()).isTrue();
        assertThat(document.lines().get(0)).isEqualTo("One-liner");
    }

    @Test
    void frontMatterOnlyDocument()
    {
        var document = newDocument("document",
                List.of("---", "foo: bar", "---"));
        assertThat(document.title()).isEqualTo("document");
        assertThat(document.frontMatter().string("foo", null)).isEqualTo("bar");
        assertThat(document.fragments().size()).isEqualTo(1);
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
        assertThat(document.title()).isEqualTo("title");
        assertThat(document.frontMatter().integer("priority", -1)).isEqualTo(100);
        assertThat(document.frontMatter().date("date", null).toString()).isEqualTo(
                "Tue Nov 30 00:00:00 CET 1976");
        assertThat(document.fragment(1).content()).isEqualTo(
                "# title\n\n## foo bar\n\nlorem ipsum");
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
        assertThat(document.title()).isEqualTo("document");
        assertThat(document.frontMatter().isEmpty()).isTrue();
        assertThat(document.fragments().size()).isEqualTo(4);
        assertThat(document.fragment(1)).isInstanceOf(Section.class);
        assertThat(document.fragment(2)).isInstanceOf(Section.class);
        var section = (Section) document.fragment(2);
        assertThat(section.fragments().size()).isEqualTo(2);
    }

    @Test
    void emptyTextsArePruned()
    {
        var document = newDocument("document", document("""
                ## Section 1

                ### Subsection 1
                """));
        var section = (Section) document.fragment(1);
        assertThat(section.fragments().size()).isEqualTo(1);
        assertThat(section.fragment(0)).isInstanceOf(Section.class);
    }

    @Test
    void unclosedYamlFrontMatter()
    {
        String text = """
                ---
                foo: bar
                        
                # Title""";
        var document = newDocument("document", document(text));
        assertThat(document.title()).isEqualTo("document");
        assertThat(document.content()).isEqualTo(text);
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
        assertThat(document.frontMatter().isEmpty()).isTrue();
        assertThat(document.title()).isEqualTo("Title");
        assertThat(document.content()).isEqualTo("---\n42\n---\n# Title");
    }

    private List<String> document(String text)
    {
        return text.lines().collect(toList());
    }
}
package nl.ulso.obsidian.watcher.vault;

import nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static nl.ulso.obsidian.watcher.vault.SimpleMarkdownTokenizer.TokenType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class SimpleMarkdownTokenizerTest
{
    @Test
    void emptyDocument()
    {
        var tokens = new SimpleMarkdownTokenizer(emptyList());
        assertThat(tokens.iterator().next().tokenType()).isEqualTo(END_OF_DOCUMENT);
    }

    @Test
    void validFrontMatter()
    {
        assertSame("""
                        ---
                        foo: bar
                        ---
                        """,
                FRONT_MATTER, FRONT_MATTER, FRONT_MATTER, END_OF_DOCUMENT);
    }

    @Test
    void frontMatterBelowTopIsJustText()
    {
        assertSame("""
                        Line 1
                        ---
                        foo: bar
                        ---""",
                TEXT, TEXT, TEXT, TEXT, END_OF_DOCUMENT);
    }

    @Test
    void plainMarkdown()
    {
        assertSame("""
                        # Title
                                        
                        ## Section
                                        
                        ### Subsection
                                        
                        Text
                                        
                        """,
                HEADER, TEXT, HEADER, TEXT, HEADER, TEXT, TEXT, TEXT, END_OF_DOCUMENT);
    }

    @Test
    void codeInMarkdown()
    {
        assertSame("""
                        ```java
                        public static void main(String[] arguments) { }
                        ```
                        """,
                TEXT, TEXT, TEXT, END_OF_DOCUMENT);
    }

    @Test
    void markdownIncodeInMarkdown()
    {
        assertSame("""
                        ```markdown
                        # Section
                        ```
                        """,
                TEXT, TEXT, TEXT, END_OF_DOCUMENT);
    }

    private void assertSame(String input, TokenType... types)
    {
        assertSame(document(input), types);
    }

    private void assertSame(List<String> input, TokenType... types)
    {
        var tokens = new SimpleMarkdownTokenizer(input);
        var i = 0;
        try
        {
            for (var token : tokens)
            {
                assertThat(token.tokenType()).isEqualTo(types[i]);
                assertThat(token.lineIndex()).isEqualTo(i);
                i++;
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            fail("More lines in actual input than expected");
        }
    }

    private List<String> document(String text)
    {
        return text.lines().collect(Collectors.toList());
    }
}
package nl.ulso.macu.vault;

import nl.ulso.macu.vault.MarkdownTokenizer.TokenType;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static nl.ulso.macu.vault.MarkdownTokenizer.TokenType.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class MarkdownTokenizerTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void emptyDocument()
    {
        var tokens = new MarkdownTokenizer(emptyList());
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
                CODE, CODE, CODE, END_OF_DOCUMENT);
    }

    @Test
    void markdownIncodeInMarkdown()
    {
        assertSame("""
                        ```markdown
                        # Section
                        ```
                        """,
                CODE, CODE, CODE, END_OF_DOCUMENT);
    }

    @Test
    void shortQuery()
    {
        assertSame("""
                        <!--query CONFIGURATION -->
                        OUTPUT
                        <!--/query-->
                        """,
                QUERY, QUERY, QUERY, END_OF_DOCUMENT);
    }

    @Test
    void longQuery()
    {
        assertSame("""
                        <!--query
                        LINE 1
                        -->
                        OUTPUT
                        <!--/query-->
                        """,
                QUERY, QUERY, QUERY, QUERY, QUERY, END_OF_DOCUMENT);
    }

    @Test
    void queryNoOutput()
    {
        assertSame("""
                        <!--query:TYPE CONFIGURATION-->
                        <!--/query:HASH-->
                        """,
                QUERY, QUERY, END_OF_DOCUMENT);
    }

    private void assertSame(String input, TokenType... types)
    {
        assertSame(document(input), types);
    }

    private void assertSame(List<String> input, TokenType... types)
    {
        var tokens = new MarkdownTokenizer(input);
        var i = 0;
        try
        {
            for (var token : tokens)
            {
                softly.assertThat(token.tokenType()).isEqualTo(types[i]);
                softly.assertThat(token.lineIndex()).isEqualTo(i);
                i++;
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            softly.fail("More lines in actual input than expected");
        }
    }

    private List<String> document(String text)
    {
        return text.lines().collect(Collectors.toList());
    }
}
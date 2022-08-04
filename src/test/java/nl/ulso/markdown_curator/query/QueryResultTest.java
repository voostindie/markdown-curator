package nl.ulso.markdown_curator.query;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

@ExtendWith(SoftAssertionsExtension.class)
class QueryResultTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    private final QueryResultFactory factory = new QueryResultFactory();

    @Test
    void error()
    {
        var error = factory.error("error");
        softly.assertThat(error.toMarkdown()).contains("error");
    }

    @Test
    void empty()
    {
        softly.assertThat(factory.empty().toMarkdown()).isEqualTo("No results");
    }

    @Test
    void tableNoResults()
    {
        var table = factory.table(List.of("1", "2"), Collections.emptyList());
        softly.assertThat(table.toMarkdown()).isEqualTo("No results");
    }

    @Test
    void tableWithResults()
    {
        var table = factory.table(List.of("Title", "Year"),
                List.of(Map.of("Title", "No Time To Die", "Year", "2021"),
                        Map.of("Title", "Spectre", "Year", "2015"),
                        Map.of("Title", "Skyfall", "Year", "2012")));
        softly.assertThat(table.toMarkdown()).isEqualTo("""
                | Title          | Year |
                | -------------- | ---- |
                | No Time To Die | 2021 |
                | Spectre        | 2015 |
                | Skyfall        | 2012 |

                (*3 results*)
                """);
    }

    @Test
    void tableWithMissingColumn()
    {
        var table = factory.table(
                List.of("Title"),
                List.of(Map.of("Name", "No Time To Die"))
        );
        softly.assertThat(table.toMarkdown()).isEqualTo("""
                | Title |
                | ----- |
                |       |

                (*1 result*)
                """);
    }

    @Test
    void listNoResults()
    {
        var list = factory.unorderedList(Collections.emptyList());
        softly.assertThat(list.toMarkdown()).isEqualTo("No results");
    }

    @Test
    void listWithResults()
    {
        var list = factory.unorderedList(List.of("Foo", "Bar"));
        softly.assertThat(list.toMarkdown()).isEqualTo("""
                - Foo
                - Bar
                """);
    }
}

package nl.ulso.markdown_curator.query;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class QueryResultTest
{
    private final QueryResultFactory factory = new QueryResultFactory();

    @Test
    void error()
    {
        var error = factory.error("error");
        assertThat(error.toMarkdown()).contains("error");
    }

    @Test
    void empty()
    {
        assertThat(factory.empty().toMarkdown()).isEqualTo("No results");
    }

    @Test
    void tableNoResults()
    {
        var table = factory.table(List.of("1", "2"), Collections.emptyList());
        assertThat(table.toMarkdown()).isEqualTo("No results");
    }

    @Test
    void tableWithResults()
    {
        var table = factory.table(List.of("Title", "Year"),
                List.of(Map.of("Title", "No Time To Die", "Year", "2021"),
                        Map.of("Title", "Spectre", "Year", "2015"),
                        Map.of("Title", "Skyfall", "Year", "2012")));
        assertThat(table.toMarkdown()).isEqualTo("""
                
                | Title          | Year |
                | -------------- | ---- |
                | No Time To Die | 2021 |
                | Spectre        | 2015 |
                | Skyfall        | 2012 |
                """);
    }

    @Test
    void tableWithMissingColumn()
    {
        var table = factory.table(
                List.of("Title"),
                List.of(Map.of("Name", "No Time To Die"))
        );
        assertThat(table.toMarkdown()).isEqualTo("""
                
                | Title |
                | ----- |
                |       |
                """);
    }

    @Test
    void listNoResults()
    {
        var list = factory.unorderedList(Collections.emptyList());
        assertThat(list.toMarkdown()).isEqualTo("No results");
    }

    @Test
    void listWithResults()
    {
        var list = factory.unorderedList(List.of("Foo", "Bar"));
        assertThat(list.toMarkdown()).isEqualTo("""
                - Foo
                - Bar
                """);
    }

    @Test
    void slowQueryOne()
    {
        var list = factory.withPerformanceWarning().unorderedList(List.of("Foo"));
        assertThat(list.toMarkdown()).isEqualTo("""
                - Foo
                                
                <!-- WARNING: do not overuse this query; it is slow! -->
                """.trim());
    }

    @Test
    void slowQueryTwo()
    {
        var list = factory.withPerformanceWarning(factory.unorderedList(List.of("Foo")));
        assertThat(list.toMarkdown()).isEqualTo("""
                - Foo
                                
                <!-- WARNING: do not overuse this query; it is slow! -->
                """.trim());
    }
}

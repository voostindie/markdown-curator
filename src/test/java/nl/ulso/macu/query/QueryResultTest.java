package nl.ulso.macu.query;

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


    @Test
    void failure()
    {
        var failure = QueryResult.failure("error");
        softly.assertThat(failure.isSuccess()).isFalse();
        softly.assertThat(failure.toMarkdown()).contains("error");
    }

    @Test
    void tableNoResults()
    {
        var table = QueryResult.table(List.of("1", "2"), Collections.emptyList());
        softly.assertThat(table.isSuccess()).isTrue();
        softly.assertThat(table.toMarkdown()).contains("No results");
    }

    @Test
    void tableWithResults()
    {
        var table = QueryResult.table(List.of("Title", "Year"),
                List.of(Map.of("Title", "No Time To Die", "Year", "2021"),
                        Map.of("Title", "Spectre", "Year", "2015"),
                        Map.of("Title", "Skyfall", "Year", "2012")));
        softly.assertThat(table.isSuccess()).isTrue();
        softly.assertThat(table.toMarkdown()).isEqualTo("""
                | Title          | Year |
                | -------------- | ---- |
                | No Time To Die | 2021 |
                | Spectre        | 2015 |
                | Skyfall        | 2012 |
                """.trim());
    }

    @Test
    void tableWithMissingColumn()
    {
        var table = QueryResult.table(
                List.of("Title"),
                List.of(Map.of("Name", "No Time To Die"))
        );
        softly.assertThat(table.toMarkdown()).isEqualTo("""
                | Title |
                | ----- |
                |       |
                """.trim());
    }

    @Test
    void listNoResults()
    {
        var list = QueryResult.list(Collections.emptyList());
        softly.assertThat(list.isSuccess()).isTrue();
        softly.assertThat(list.toMarkdown()).contains("No results");
    }

    @Test
    void listWithResults()
    {
        var list = QueryResult.list(List.of("Foo", "Bar"));
        softly.assertThat(list.isSuccess()).isTrue();
        softly.assertThat(list.toMarkdown()).isEqualTo("""
                - Foo
                - Bar
                """.trim());
    }
}
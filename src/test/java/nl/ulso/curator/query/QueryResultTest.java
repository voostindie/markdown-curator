package nl.ulso.curator.query;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static nl.ulso.curator.query.QueryResultFactory.Alignment.CENTER;
import static nl.ulso.curator.query.QueryResultFactory.Alignment.LEFT;
import static nl.ulso.curator.query.QueryResultFactory.Alignment.RIGHT;
import static org.assertj.core.api.Assertions.assertThat;

class QueryResultTest
{
    private final DefaultQueryResultFactory factory = new DefaultQueryResultFactory();

    @Test
    void error()
    {
        var error = (ErrorResult) factory.error("error");
        assertThat(error.errorMessage()).contains("error");
    }

    @Test
    void empty()
    {
        var empty = (EmptyResult) factory.empty();
        assertThat(empty.outputMessage()).isEqualTo("No results");
    }

    @Test
    void tableNoResults()
    {
        var table = (EmptyResult) factory.table(List.of("1", "2"), emptyList());
        assertThat(table.outputMessage()).isEqualTo("No results");
    }

    @Test
    void tableWithResults()
    {
        var table = (TableResult) factory.table(List.of("Title", "Year"),
            List.of(Map.of("Title", "No Time To Die", "Year", "2021"),
                Map.of("Title", "Spectre", "Year", "2015"),
                Map.of("Title", "Skyfall", "Year", "2012")
            )
        );
        var output = new TableMarkdownFormatter().format(table);
        assertThat(output).isEqualTo("""
            
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
        var table = (TableResult) factory.table(
            List.of("Title"),
            List.of(Map.of("Name", "No Time To Die"))
        );
        var output = new TableMarkdownFormatter().format(table);
        assertThat(output).isEqualTo("""
            
            | Title |
            | ----- |
            |       |
            
            """);
    }

    @Test
    void tableWithAlignments()
    {
        var table = (TableResult) factory.table(
            List.of("Title", "Year", "Rating"),
            List.of(LEFT, CENTER, RIGHT),
            List.of(Map.of("Title", "No Time To Die", "Year", "2021", "Rating", "7.3"),
                Map.of("Title", "Spectre", "Year", "2015", "Rating", "6.8"),
                Map.of("Title", "Skyfall", "Year", "2012", "Rating", "7.8")
            )
        );
        var output = new TableMarkdownFormatter().format(table);
        assertThat(output).isEqualTo("""
            
            | Title          | Year | Rating |
            | -------------- | :--: | -----: |
            | No Time To Die | 2021 | 7.3    |
            | Spectre        | 2015 | 6.8    |
            | Skyfall        | 2012 | 7.8    |
            
            """);
    }

    @Test
    void listNoResults()
    {
        var list = (EmptyResult) factory.unorderedList(emptyList());
        assertThat(list.outputMessage()).isEqualTo("No results");
    }

    @Test
    void listWithResults()
    {
        var list = (UnorderedListResult) factory.unorderedList(List.of("Foo", "Bar"));
        var output = new UnorderedListMarkdownFormatter().format(list);
        assertThat(output).isEqualTo("""
            - Foo
            - Bar
            """);
    }
}

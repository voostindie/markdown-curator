package nl.ulso.curator.query;

import org.junit.jupiter.api.Test;

import java.util.*;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static org.assertj.core.api.Assertions.assertThat;

class TableJsonFormatterTest
{
    private final TableJsonFormatter formatter = new TableJsonFormatter();

    @Test
    void queryResultType()
    {
        assertThat(formatter.queryResultType()).isEqualTo(TableResult.class);
    }

    @Test
    void outputFormat()
    {
        assertThat(formatter.outputFormat()).isEqualTo(JSON);
    }

    @Test
    void formatTableWithRows()
    {
        var table = new TableResult(
            List.of("Title", "Year"),
            List.of(QueryResultFactory.Alignment.LEFT, QueryResultFactory.Alignment.LEFT),
            List.of(
                Map.of("Title", "No Time To Die", "Year", "2021"),
                Map.of("Title", "Spectre", "Year", "2015")
            )
        );
        var json = formatter.format(table);
        assertThat(json).isEqualTo("{\"items\":[{\"Title\":\"No Time To Die\",\"Year\":\"2021\"},{\"Title\":\"Spectre\",\"Year\":\"2015\"}]}");
    }

    @Test
    void formatTableWithoutRows()
    {
        var table = new TableResult(
            List.of("Title", "Year"),
            List.of(QueryResultFactory.Alignment.LEFT, QueryResultFactory.Alignment.LEFT),
            Collections.emptyList()
        );
        var json = formatter.format(table);
        assertThat(json).isEqualTo("{\"items\":[]}");
    }
}

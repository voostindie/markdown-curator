package nl.ulso.curator.query;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static org.assertj.core.api.Assertions.assertThat;

class UnorderedListJsonFormatterTest
{
    private final UnorderedListJsonFormatter formatter = new UnorderedListJsonFormatter();

    @Test
    void queryResultType()
    {
        assertThat(formatter.queryResultType()).isEqualTo(UnorderedListResult.class);
    }

    @Test
    void outputFormat()
    {
        assertThat(formatter.outputFormat()).isEqualTo(JSON);
    }

    @Test
    void formatListWithItems()
    {
        var list = new UnorderedListResult(List.of("Item 1", "Item 2", "Item 3"));
        var json = formatter.format(list);
        assertThat(json).isEqualTo("{\"items\":[\"Item 1\",\"Item 2\",\"Item 3\"]}");
    }

    @Test
    void formatEmptyList()
    {
        var list = new UnorderedListResult(Collections.emptyList());
        var json = formatter.format(list);
        assertThat(json).isEqualTo("{\"items\":[]}");
    }
}

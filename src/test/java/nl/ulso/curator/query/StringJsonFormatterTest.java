package nl.ulso.curator.query;

import org.junit.jupiter.api.Test;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static org.assertj.core.api.Assertions.assertThat;

class StringJsonFormatterTest
{
    private final StringJsonFormatter formatter = new StringJsonFormatter();

    @Test
    void queryResultType()
    {
        assertThat(formatter.queryResultType()).isEqualTo(StringResult.class);
    }

    @Test
    void outputFormat()
    {
        assertThat(formatter.outputFormat()).isEqualTo(JSON);
    }

    @Test
    void formatString()
    {
        var result = new StringResult("Hello, world!");
        var json = formatter.format(result);
        assertThat(json).isEqualTo("{\"value\":\"Hello, world!\"}");
    }

    @Test
    void formatEmptyString()
    {
        var result = new StringResult("");
        var json = formatter.format(result);
        assertThat(json).isEqualTo("{\"value\":\"\"}");
    }

    @Test
    void formatSpecialCharacters()
    {
        var result = new StringResult("Line 1\nLine 2\t\"quoted\"");
        var json = formatter.format(result);
        assertThat(json).isEqualTo("{\"value\":\"Line 1\\nLine 2\\t\\\"quoted\\\"\"}");
    }
}

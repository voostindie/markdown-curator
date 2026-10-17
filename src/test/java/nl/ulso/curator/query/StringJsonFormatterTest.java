package nl.ulso.curator.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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

    @ParameterizedTest
    @MethodSource("stringResults")
    void formatString(String input, String expectedJson)
    {
        var result = new StringResult(input);
        var json = formatter.format(result);
        assertThat(json).isEqualTo(expectedJson);
    }

    private static Stream<Arguments> stringResults()
    {
        return Stream.of(
            Arguments.of("Hello, world!", "{\"value\":\"Hello, world!\"}"),
            Arguments.of("", "{\"value\":\"\"}"),
            Arguments.of("Line 1\nLine 2\t\"quoted\"", "{\"value\":\"Line 1\\nLine 2\\t\\\"quoted\\\"\"}")
        );
    }
}

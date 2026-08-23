package nl.ulso.curator.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultQueryResultFormatterRegistryTest
{
    private QueryResultFormatterRegistry registry;

    @BeforeEach
    void setUp()
    {
        var formatters = Set.of(
            new StringMarkdownFormatter(),
            new UnorderedListMarkdownFormatter(),
            new TableMarkdownFormatter(),
            new ErrorMarkdownFormatter()
        );
        registry = new DefaultQueryResultFormatterRegistry(formatters);
    }

    @Test
    void unknownOutputFormat()
    {
        var output = registry.format(
            new StringResult("unknown"),
            new OutputFormat("UNKNOWN", "application/unknown")
        );
        assertThat(output)
            .contains("Error")
            .contains("Unknown output format 'UNKNOWN'");
    }

    @Test
    void unknownQueryResult()
    {
        var output = registry.format(
            new DummyQueryResult(),
            MARKDOWN
        );
        assertThat(output)
            .contains("Error")
            .contains(
                "No output formatter for 'markdown' available for query result type " +
                "'DummyQueryResult'");
    }

    @Test
    void stringResult()
    {
        var output = registry.format(
            new StringResult("Hello, world"),
            MARKDOWN
        );
        assertThat(output).contains("Hello, world");
    }

    /// No specific formatter exists for the [EmptyResult], but the type extends [StringResult],
    /// so the [StringMarkdownFormatter] should kick in.
    @Test
    void emptyResult()
    {
        var output = registry.format(
            new EmptyResult("empty"),
            MARKDOWN
        );
        assertThat(output).contains("empty");
    }

    private static final class DummyQueryResult
        implements QueryResult
    {
    }
}

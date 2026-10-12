package nl.ulso.curator.addon.alfred;

import nl.ulso.curator.query.*;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.curator.change.Changelog.emptyChangelog;
import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static nl.ulso.curator.query.QueryTestModule.createQueryResultFactory;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class AlfredQueryTest
{
    private final QueryResultFormatterRegistry formatterRegistry =
        QueryTestModule.createQueryResultFormatterRegistry();
    private AlfredQuery query;

    @BeforeEach
    void setUp()
    {
        query = new AlfredQuery(
            AlfredSettings.builder().projectsPath("Projects").build(), createQueryResultFactory()
        );
    }

    @Test
    void name()
    {
        assertThat(query.name()).isEqualTo("alfred");
    }

    @Test
    void description()
    {
        assertThat(query.description()).isEqualTo("Output settings for the Alfred workflow");
    }

    @Test
    void supportedConfiguration()
    {
        assertThat(query.supportedConfiguration()).isEmpty();
    }

    @Test
    void isImpactedBy()
    {
        assertThat(query.isImpactedBy(emptyChangelog(), null)).isFalse();
    }

    @Test
    void runReturnsMarkdownTable()
    {
        var definition = new QueryDefinitionStub(query, null);
        var result = query.run(definition);
        var output = formatterRegistry.format(result, MARKDOWN);
        assertThat(output).isEqualTo("""
            
            | Name     | Value |
            | -------- | ----- |
            | projects | Projects |
            
            """);
    }

    @Test
    void runReturnsJsonTable()
    {
        var definition = new QueryDefinitionStub(query, null);
        var result = query.run(definition);
        var output = formatterRegistry.format(result, JSON);
        assertThat(output).isEqualTo("""
            {"items":[{"name":"projects","value":"Projects"}]}""");
    }
}

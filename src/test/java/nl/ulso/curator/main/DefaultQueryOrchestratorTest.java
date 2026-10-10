package nl.ulso.curator.main;

import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.VaultStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static java.util.Collections.emptyMap;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.change.Reset.RESET;
import static nl.ulso.curator.query.QueryTestModule.createQueryCatalog;
import static nl.ulso.curator.query.QueryTestModule.createQueryResultFactory;
import static nl.ulso.curator.query.QueryTestModule.createQueryResultFormatterRegistry;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultQueryOrchestratorTest
{
    private static final QueryResultFactory RESULT_FACTORY = createQueryResultFactory();
    private VaultStub vault;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
    }

    @Test
    void defaultOutputFormatIsMarkdown()
    {
        var document = vault.addDocument("test", """
            <!--query:dummy-->
            <!--/query-->
            """);
        var orchestrator = createOrchestrator(new DummyQuery("dummy", RESULT_FACTORY.string("Hello, world!")));
        var updates = orchestrator.runFor(changelogFor(RESET));

        assertThat(updates).hasSize(1);
        var update = updates.iterator().next();
        assertThat(update.document()).isEqualTo(document);
        assertThat(update.queryOutputs()).hasSize(1);
        var output = update.queryOutputs().getFirst();
        assertThat(output.content()).isEqualTo("Hello, world!");
    }

    @Test
    void overrideOutputFormatWithJson()
    {
        var document = vault.addDocument("test", """
            <!--query:dummy
            _format: json
            -->
            <!--/query-->
            """);
        var orchestrator = createOrchestrator(new DummyQuery("dummy", RESULT_FACTORY.string("Hello, world!")));
        var updates = orchestrator.runFor(changelogFor(RESET));

        assertThat(updates).hasSize(1);
        var update = updates.iterator().next();
        assertThat(update.document()).isEqualTo(document);
        assertThat(update.queryOutputs()).hasSize(1);
        var output = update.queryOutputs().getFirst();
        assertThat(output.content()).isEqualTo("""
            ```json
            {"value":"Hello, world!"}
            ```""");
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "JSON", "Json", "jSoN"})
    void overrideOutputFormatIsCaseInsensitive(String formatProperty)
    {
        vault.addDocument("test", """
            <!--query:dummy
            _format: %s
            -->
            <!--/query-->
            """.formatted(formatProperty));
        var orchestrator = createOrchestrator(new DummyQuery("dummy", RESULT_FACTORY.string("Hello, world!")));
        var updates = orchestrator.runFor(changelogFor(RESET));

        assertThat(updates).hasSize(1);
        var output = updates.iterator().next().queryOutputs().getFirst();
        assertThat(output.content()).isEqualTo("""
            ```json
            {"value":"Hello, world!"}
            ```""");
    }

    @Test
    void explicitMarkdownFormatIsNotCodeFenced()
    {
        vault.addDocument("test", """
            <!--query:dummy
            _format: markdown
            -->
            <!--/query-->
            """);
        var orchestrator = createOrchestrator(new DummyQuery("dummy", RESULT_FACTORY.string("Hello, world!")));
        var updates = orchestrator.runFor(changelogFor(RESET));

        assertThat(updates).hasSize(1);
        var output = updates.iterator().next().queryOutputs().getFirst();
        assertThat(output.content()).isEqualTo("Hello, world!");
    }

    @Test
    void unknownOutputFormatFallsBackToMarkdown()
    {
        vault.addDocument("test", """
            <!--query:dummy
            _format: unknown
            -->
            <!--/query-->
            """);
        var orchestrator = createOrchestrator(new DummyQuery("dummy", RESULT_FACTORY.string("Hello, world!")));
        var updates = orchestrator.runFor(changelogFor(RESET));

        assertThat(updates).hasSize(1);
        var output = updates.iterator().next().queryOutputs().getFirst();
        assertThat(output.content()).isEqualTo("Hello, world!");
    }

    @Test
    void overrideWithCustomRegisteredOutputFormat()
    {
        vault.addDocument("test", """
            <!--query:dummy
            _format: custom
            -->
            <!--/query-->
            """);
        var customFormat = new OutputFormat("custom", "text/custom");
        var customFormatter = new CustomFormatter(customFormat);
        var orchestrator = createOrchestratorWithFormatterRegistry(
            createQueryResultFormatterRegistry(Set.of(customFormatter)),
            new DummyQuery("dummy", new CustomResult("custom content"))
        );
        var updates = orchestrator.runFor(changelogFor(RESET));

        assertThat(updates).hasSize(1);
        var output = updates.iterator().next().queryOutputs().getFirst();
        assertThat(output.content()).isEqualTo("""
            ```custom
            CUSTOM: custom content
            ```""");
    }

    private DefaultQueryOrchestrator createOrchestrator(Query... queries)
    {
        return createOrchestratorWithFormatterRegistry(createQueryResultFormatterRegistry(), queries);
    }

    private DefaultQueryOrchestrator createOrchestratorWithFormatterRegistry(
        QueryResultFormatterRegistry formatterRegistry, Query... queries)
    {
        var catalog = createQueryCatalog(Set.of(queries));
        return new DefaultQueryOrchestrator(vault, catalog, formatterRegistry, Collections::emptyMap);
    }

    private static final class DummyQuery
        implements Query
    {
        private final String name;
        private final QueryResult result;

        DummyQuery(String name, QueryResult result)
        {
            this.name = name;
            this.result = result;
        }

        @Override
        public String name()
        {
            return name;
        }

        @Override
        public String description()
        {
            return "Dummy query for testing";
        }

        @Override
        public Map<String, String> supportedConfiguration()
        {
            return emptyMap();
        }

        @Override
        public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
        {
            return true;
        }

        @Override
        public QueryResult run(QueryDefinition definition)
        {
            return result;
        }
    }

    private record CustomResult(String text) implements QueryResult {}

    private static final class CustomFormatter
        implements QueryResultFormatter<CustomResult>
    {
        private final OutputFormat outputFormat;

        CustomFormatter(OutputFormat outputFormat)
        {
            this.outputFormat = outputFormat;
        }

        @Override
        public Class<CustomResult> queryResultType()
        {
            return CustomResult.class;
        }

        @Override
        public OutputFormat outputFormat()
        {
            return outputFormat;
        }

        @Override
        public String format(CustomResult result)
        {
            return "CUSTOM: " + result.text();
        }
    }
}

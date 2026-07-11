package nl.ulso.curator.main;

import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.Folder;
import nl.ulso.dictionary.Dictionary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class ChangeProcessorGraphQueryTest
{
    private ChangeProcessorGraphQuery query;

    @BeforeEach
    void setUp()
    {
        var processors = new TreeSet<ChangeProcessor>(List.of(
            new ChangeProcessorOrchestratorTest.ChangeProcessorStub(1)
                .consuming(Folder.class)
                .producing(String.class),
            new ChangeProcessorOrchestratorTest.ChangeProcessorStub(2)
                .consuming(Document.class)
                .producing(Integer.class),
            new ChangeProcessorOrchestratorTest.ChangeProcessorStub(3)
                .consuming(String.class)
                .requiring(Document.class)
                .producing(Integer.class),
            new ChangeProcessorOrchestratorTest.ChangeProcessorStub(4)
                .consuming(Integer.class),
            new ChangeProcessorOrchestratorTest.ChangeProcessorStub(5)
                .consuming(Integer.class)
        ));
        var orchestrator = new DefaultChangeProcessorOrchestrator(processors, null);
        this.query =
            new ChangeProcessorGraphQuery(orchestrator, new StringOnlyQueryResultFactory());
    }

    @Test
    void testName()
    {
        assertThat(query.name()).isEqualTo("processorgraph");
    }

    @Test
    void testDescription()
    {
        assertThat(query.description()).contains("Generates a Mermaid diagram");
    }

    @Test
    void testConfiguration()
    {
        assertThat(query.supportedConfiguration().keySet())
            .containsExactlyInAnyOrder("edges", "orientation");
    }

    @Test
    void testDefaultConfiguration()
    {
        var result =
            query.run(QueryDefinitionStub.forConfiguration(emptyMap())).toMarkdown();
        assertThat(result)
            .contains("graph LR")
            .contains(" ==> ")
            .doesNotContain(" --> ");
    }

    @Test
    void testOrientation()
    {
        var result =
            query.run(QueryDefinitionStub.forConfiguration(Map.of("orientation", "tb")))
                .toMarkdown();
        assertThat(result)
            .contains("graph TB");
    }

    @Test
    void testOrder()
    {
        var result = query.run(QueryDefinitionStub.forConfiguration(Map.of(
            "edges", "order"
        ))).toMarkdown();
        assertThat(result)
            .contains("Stub-1 ==> Stub-2")
            .contains("Stub-2 ==> Stub-3")
            .contains("Stub-3 ==> Stub-4")
            .contains("Stub-4 ==> Stub-5");
    }

    @Test
    void testConsume()
    {
        var result = query.run(QueryDefinitionStub.forConfiguration(Map.of(
            "edges", "consume"
        ))).toMarkdown();
        assertThat(result)
            .contains("Folder --> Stub-1")
            .contains("Document --> Stub-2")
            .contains("String --> Stub-3")
            .contains("Integer --> Stub-4")
            .contains("Integer --> Stub-5");
    }

    @Test
    void testProduce()
    {
        var result = query.run(QueryDefinitionStub.forConfiguration(Map.of(
            "edges", "produce"
        ))).toMarkdown();
        assertThat(result)
            .contains("Stub-1 --> String")
            .contains("Stub-2 --> Integer")
            .contains("Stub-3 --> Integer");
    }

    @Test
    void testRequire()
    {
        var result = query.run(QueryDefinitionStub.forConfiguration(Map.of(
            "edges", "require"
        ))).toMarkdown();
        assertThat(result)
            .contains("Document -.-> Stub-3");
    }

    @Test
    void testAll()
    {
        var result = query.run(QueryDefinitionStub.forConfiguration(Map.of(
            "edges", List.of("order", "produce", "consume", "require")
        ))).toMarkdown();
        assertThat(result)
            .contains(" ==> ")
            .contains("Folder --> Stub-1")
            .contains("Stub-1 --> String")
            .contains("Document -.-> Stub-3");
    }

    private static class QueryDefinitionStub
        implements QueryDefinition
    {
        private final Dictionary configuration;

        QueryDefinitionStub(Map<String, Object> configuration)
        {
            this.configuration = Dictionary.mapDictionary(configuration);
        }

        static QueryDefinition forConfiguration(Map<String, Object> configuration)
        {
            return new QueryDefinitionStub(configuration);
        }

        @Override
        public String queryName()
        {
            return "";
        }

        @Override
        public Dictionary configuration()
        {
            return configuration;
        }

        @Override
        public Document document()
        {
            return null;
        }

        @Override
        public String outputHash()
        {
            return "";
        }
    }

    private static class StringOnlyQueryResultFactory
        implements QueryResultFactory
    {
        @Override
        public QueryResult empty()
        {
            return null;
        }

        @Override
        public QueryResult error(String errorMessage)
        {
            return null;
        }

        @Override
        public QueryResult table(List<String> columns, List<Map<String, String>> rows)
        {
            return null;
        }

        @Override
        public QueryResult table(
            List<String> columns, List<Alignment> alignments,
            List<Map<String, String>> rows)
        {
            return null;
        }

        @Override
        public QueryResult unorderedList(List<String> rows)
        {
            return null;
        }

        @Override
        public QueryResult string(String output)
        {
            return () -> output;
        }

        @Override
        public QueryResult withPerformanceWarning(QueryResult slowQueryResult)
        {
            return null;
        }

        @Override
        public QueryResultFactory withPerformanceWarning()
        {
            return null;
        }
    }
}

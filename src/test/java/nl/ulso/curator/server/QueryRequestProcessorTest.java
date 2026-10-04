package nl.ulso.curator.server;

import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static nl.ulso.dictionary.Dictionary.mapDictionary;
import static org.assertj.core.api.Assertions.assertThat;

class QueryRequestProcessorTest
{
    private CuratorStub curator;
    private QueryRequestProcessor processor;

    @BeforeEach
    void setUp()
    {
        curator = new CuratorStub();
        processor = new QueryRequestProcessor(Map.of("testCurator", curator));
    }

    @Test
    void pathTemplate()
    {
        assertThat(processor.pathTemplate()).isEqualTo("/curators/{curator}/queries/{query}");
    }

    @Test
    void successfulQueryExecution()
    {
        var capturedDefinition = new AtomicReference<QueryDefinition>();
        var query = new Query()
        {
            @Override
            public String name()
            {
                return "testQuery";
            }

            @Override
            public String description()
            {
                return "Test Query";
            }

            @Override
            public Map<String, String> supportedConfiguration()
            {
                return Map.of("filter", "filter value");
            }

            @Override
            public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
            {
                return false;
            }

            @Override
            public QueryResult run(QueryDefinition definition)
            {
                capturedDefinition.set(definition);
                return new QueryResult() {};
            }
        };

        curator.registerQuery(query);

        var pathParams = mapDictionary(Map.of(
            "curator", "testCurator",
            "query", "testQuery"
        ));
        var queryParams = mapDictionary(Map.of("filter", "active"));

        var result = processor.process(pathParams, queryParams, JSON);

        assertThat(result.status()).isEqualTo(Status.OK);
        assertThat(result.outputFormat()).isEqualTo(JSON);
        assertThat(result.output()).contains("formatted:json");

        var definition = capturedDefinition.get();
        assertThat(definition).isNotNull();
        assertThat(definition.queryName()).isEqualTo("testQuery");
        assertThat(definition.configuration().string("filter", null)).isEqualTo("active");
        assertThat(definition.document()).isEqualTo(Document.EMPTY_DOCUMENT);
    }

    @Test
    void defaultQueryNameIsHelpWhenNotProvided()
    {
        var helpQuery = new Query()
        {
            @Override
            public String name()
            {
                return "help";
            }

            @Override
            public String description()
            {
                return "Help Query";
            }

            @Override
            public Map<String, String> supportedConfiguration()
            {
                return Map.of();
            }

            @Override
            public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
            {
                return false;
            }

            @Override
            public QueryResult run(QueryDefinition definition)
            {
                return new QueryResult() {};
            }
        };

        curator.registerQuery(helpQuery);

        var pathParams = mapDictionary(Map.of("curator", "testcurator"));
        var result = processor.process(pathParams, emptyDictionary(), JSON);

        assertThat(result.status()).isEqualTo(Status.OK);
    }

    @Test
    void unsupportedOutputFormatReturnsServerError()
    {
        var query = new Query()
        {
            @Override
            public String name()
            {
                return "unsupportedQuery";
            }

            @Override
            public String description()
            {
                return "Description";
            }

            @Override
            public Map<String, String> supportedConfiguration()
            {
                return Map.of();
            }

            @Override
            public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
            {
                return false;
            }

            @Override
            public QueryResult run(QueryDefinition definition)
            {
                return new QueryResult() {};
            }
        };

        curator.registerQuery(query);
        curator.setFailSupports(true);

        var pathParams = mapDictionary(Map.of(
            "curator", "testCurator",
            "query", "unsupportedQuery"
        ));

        var result = processor.process(pathParams, emptyDictionary(), MARKDOWN);

        assertThat(result.status()).isEqualTo(Status.SERVER_ERROR);
        assertThat(result.output()).contains("Unsupported output format for this query.");
    }

    @Test
    void runtimeExceptionDuringQueryExecutionReturnsServerError()
    {
        var query = new Query()
        {
            @Override
            public String name()
            {
                return "failingQuery";
            }

            @Override
            public String description()
            {
                return "Failing";
            }

            @Override
            public Map<String, String> supportedConfiguration()
            {
                return Map.of();
            }

            @Override
            public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
            {
                return false;
            }

            @Override
            public QueryResult run(QueryDefinition definition)
            {
                throw new IllegalStateException("Query execution failed abnormally");
            }
        };

        curator.registerQuery(query);

        var pathParams = mapDictionary(Map.of(
            "curator", "testCurator",
            "query", "failingQuery"
        ));

        var result = processor.process(pathParams, emptyDictionary(), JSON);

        assertThat(result.status()).isEqualTo(Status.SERVER_ERROR);
        assertThat(result.output()).contains("Query execution failed abnormally");
    }

    @Test
    void blankCuratorNameReturnsClientError()
    {
        var pathParams = mapDictionary(Map.of("curator", "   "));
        var result = processor.process(pathParams, emptyDictionary(), JSON);

        assertThat(result.status()).isEqualTo(Status.CLIENT_ERROR);
        assertThat(result.output()).contains("Curator name is blank in the path parameters.");
    }

    @Test
    void missingCuratorNameReturnsClientError()
    {
        var result = processor.process(emptyDictionary(), emptyDictionary(), JSON);

        assertThat(result.status()).isEqualTo(Status.CLIENT_ERROR);
        assertThat(result.output()).contains("Curator name is blank in the path parameters.");
    }

    @Test
    void unknownCuratorReturnsClientError()
    {
        var pathParams = mapDictionary(Map.of("curator", "nonExistentCurator"));
        var result = processor.process(pathParams, emptyDictionary(), JSON);

        assertThat(result.status()).isEqualTo(Status.CLIENT_ERROR);
        assertThat(result.output()).contains("Curator not found: nonexistentcurator");
    }
}

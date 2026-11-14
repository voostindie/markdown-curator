package nl.ulso.curator.server;

import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;
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

        var result = processor.process(pathParams, queryParams, ContentNegotiation.of(JSON));

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
        var result = processor.process(pathParams, emptyDictionary(), ContentNegotiation.of(JSON));

        assertThat(result.status()).isEqualTo(Status.OK);
    }

    @Test
    void unsupportedResultTypeFallsBackToMarkdown()
    {
        curator.registerQuery(new StubQuery("testQuery"));
        curator.setFallBackToMarkdown(true);

        var result = processor.process(queryPath("testQuery"), emptyDictionary(),
            ContentNegotiation.of(JSON));

        assertThat(result.status()).isEqualTo(Status.OK);
        assertThat(result.outputFormat()).isEqualTo(MARKDOWN);
        assertThat(result.output()).contains("formatted:markdown");
    }

    @Test
    void unknownExplicitFormatIsNotAcceptable()
    {
        curator.registerQuery(new StubQuery("testQuery"));

        var result = processor.process(queryPath("testQuery"), emptyDictionary(),
            ContentNegotiation.of(null, "csv"));

        assertThat(result.status()).isEqualTo(Status.NOT_ACCEPTABLE);
        assertThat(result.output().orElseThrow()).contains("'csv'",
            "json (application/json)", "markdown (text/markdown)");
    }

    @Test
    void unmatchedAcceptHeaderIsNotAcceptable()
    {
        curator.registerQuery(new StubQuery("testQuery"));

        var result = processor.process(queryPath("testQuery"), emptyDictionary(),
            ContentNegotiation.of("image/png", null));

        assertThat(result.status()).isEqualTo(Status.NOT_ACCEPTABLE);
    }

    @Test
    void noPreferencesSelectsJson()
    {
        curator.registerQuery(new StubQuery("testQuery"));

        var result = processor.process(queryPath("testQuery"), emptyDictionary(),
            ContentNegotiation.none());

        assertThat(result.status()).isEqualTo(Status.OK);
        assertThat(result.outputFormat()).isEqualTo(JSON);
    }

    @Test
    void formatAddedAtRuntimeIsServed()
    {
        curator.registerQuery(new StubQuery("testQuery"));
        var csv = new OutputFormat("csv", "text/csv");
        var negotiation = ContentNegotiation.of("text/csv", null);

        var before = processor.process(queryPath("testQuery"), emptyDictionary(), negotiation);
        curator.addOutputFormat(csv);
        var during = processor.process(queryPath("testQuery"), emptyDictionary(), negotiation);
        curator.removeOutputFormat(csv);
        var after = processor.process(queryPath("testQuery"), emptyDictionary(), negotiation);

        assertThat(before.status()).isEqualTo(Status.NOT_ACCEPTABLE);
        assertThat(during.status()).isEqualTo(Status.OK);
        assertThat(during.outputFormat()).isEqualTo(csv);
        assertThat(during.output()).contains("formatted:csv");
        assertThat(after.status()).isEqualTo(Status.NOT_ACCEPTABLE);
    }

    @Test
    void negotiationIsPerCurator()
    {
        var csv = new OutputFormat("csv", "text/csv");
        var withCsv = new CuratorStub();
        withCsv.addOutputFormat(csv);
        withCsv.registerQuery(new StubQuery("testQuery"));
        curator.registerQuery(new StubQuery("testQuery"));
        var multiProcessor = new QueryRequestProcessor(
            Map.of("withCsv", withCsv, "withoutCsv", curator));
        var negotiation = ContentNegotiation.of("text/csv", null);

        var ok = multiProcessor.process(
            mapDictionary(Map.of("curator", "withCsv", "query", "testQuery")),
            emptyDictionary(), negotiation);
        var notAcceptable = multiProcessor.process(
            mapDictionary(Map.of("curator", "withoutCsv", "query", "testQuery")),
            emptyDictionary(), negotiation);

        assertThat(ok.status()).isEqualTo(Status.OK);
        assertThat(ok.outputFormat()).isEqualTo(csv);
        assertThat(notAcceptable.status()).isEqualTo(Status.NOT_ACCEPTABLE);
    }

    private static Dictionary queryPath(String queryName)
    {
        return mapDictionary(Map.of("curator", "testCurator", "query", queryName));
    }

    private record StubQuery(String name)
        implements Query
    {
        @Override
        public String description()
        {
            return "Stub";
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

        var result = processor.process(pathParams, emptyDictionary(), ContentNegotiation.of(JSON));

        assertThat(result.status()).isEqualTo(Status.SERVER_ERROR);
        assertThat(result.output()).contains("Query execution failed abnormally");
    }

    @Test
    void blankCuratorNameReturnsClientError()
    {
        var pathParams = mapDictionary(Map.of("curator", "   "));
        var result = processor.process(pathParams, emptyDictionary(), ContentNegotiation.of(JSON));

        assertThat(result.status()).isEqualTo(Status.CLIENT_ERROR);
        assertThat(result.output()).contains("Curator name is blank in the path parameters.");
    }

    @Test
    void missingCuratorNameReturnsClientError()
    {
        var result = processor.process(emptyDictionary(), emptyDictionary(),
            ContentNegotiation.of(JSON));

        assertThat(result.status()).isEqualTo(Status.CLIENT_ERROR);
        assertThat(result.output()).contains("Curator name is blank in the path parameters.");
    }

    @Test
    void unknownCuratorReturnsClientError()
    {
        var pathParams = mapDictionary(Map.of("curator", "nonExistentCurator"));
        var result = processor.process(pathParams, emptyDictionary(), ContentNegotiation.of(JSON));

        assertThat(result.status()).isEqualTo(Status.CLIENT_ERROR);
        assertThat(result.output()).contains("Curator not found: nonexistentcurator");
    }
}

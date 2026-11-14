package nl.ulso.curator.server;

import nl.ulso.curator.query.OutputFormat;
import nl.ulso.dictionary.Dictionary;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.junit.jupiter.api.*;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static nl.ulso.curator.server.Result.notAcceptable;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

class JettyRequestHandlerTest
{
    private Server server;
    private LocalConnector connector;
    private AtomicReference<Dictionary> capturedPathParams;
    private AtomicReference<Dictionary> capturedQueryParams;
    private AtomicReference<OutputFormat> capturedOutputFormat;
    private AtomicReference<Set<OutputFormat>> availableFormats;

    @BeforeEach
    void setUp()
        throws Exception
    {
        server = new Server();
        connector = new LocalConnector(server);
        server.addConnector(connector);

        capturedPathParams = new AtomicReference<>();
        capturedQueryParams = new AtomicReference<>();
        capturedOutputFormat = new AtomicReference<>();
        availableFormats = new AtomicReference<>(Set.of(JSON, MARKDOWN));

        var dummyProcessor = new RequestProcessor()
        {
            @Override
            public String pathTemplate()
            {
                return "/test/{id}";
            }

            @Override
            public Result process(
                Dictionary pathParameters,
                Dictionary queryParameters,
                ContentNegotiation contentNegotiation)
            {
                capturedPathParams.set(pathParameters);
                capturedQueryParams.set(queryParameters);
                var available = availableFormats.get();
                var selected = contentNegotiation.select(available, JSON);
                capturedOutputFormat.set(selected.orElse(null));
                if (selected.isEmpty())
                {
                    return notAcceptable(contentNegotiation.describeFailure(available));
                }
                var requestedOutputFormat = selected.get();

                if ("empty".equals(pathParameters.string("id", "")))
                {
                    return Result.ok(requestedOutputFormat, "");
                }

                if ("error".equals(pathParameters.string("id", "")))
                {
                    return Result.clientError("Invalid id");
                }

                return Result.ok(requestedOutputFormat,
                    "processed:" + pathParameters.string("id", "")
                );
            }
        };

        var handler = new PathMappingsHandler();
        handler.addMapping(
            new UriTemplatePathSpec(dummyProcessor.pathTemplate()),
            new JettyRequestHandler(dummyProcessor)
        );
        server.setHandler(handler);
        server.start();
    }

    @AfterEach
    void tearDown()
        throws Exception
    {
        if (server != null)
        {
            server.stop();
        }
    }

    @Test
    void getRequestProcessedSuccessfullyWithDefaultJsonOutputFormat()
        throws Exception
    {
        var response = connector.getResponse("""
            GET /test/123?sort=asc&count=5 HTTP/1.1
            Host: localhost
            
            """);

        assertThat(response).contains(
            "HTTP/1.1 200 OK",
            "Content-Type: application/json; charset=UTF-8",
            "processed:123"
        );

        assertThat(capturedPathParams.get().string("id", "")).isEqualTo("123");
        assertThat(capturedQueryParams.get().string("sort", "")).isEqualTo("asc");
        assertThat(capturedQueryParams.get().integer("count", 0)).isEqualTo(5);
        assertThat(capturedOutputFormat.get()).isEqualTo(JSON);
    }

    @Test
    void getRequestWithAcceptMarkdownNegotiation()
        throws Exception
    {
        var response = connector.getResponse("""
            GET /test/markdown-test HTTP/1.1
            Host: localhost
            Accept: text/markdown
            
            """);

        assertThat(response).contains(
            "HTTP/1.1 200 OK",
            "Content-Type: text/markdown; charset=UTF-8",
            "processed:markdown-test"
        );
        assertThat(capturedOutputFormat.get()).isEqualTo(MARKDOWN);
    }

    @Test
    void getRequestWithUnknownAcceptHeaderIsNotAcceptable()
        throws Exception
    {
        var response = connector.getResponse("""
            GET /test/unknown-accept HTTP/1.1
            Host: localhost
            Accept: image/png
            
            """);

        assertThat(response).contains(
            "HTTP/1.1 406 Not Acceptable",
            "Content-Type: text/markdown; charset=UTF-8",
            "image/png"
        );
        assertThat(capturedOutputFormat.get()).isNull();
    }

    @Test
    void getRequestWithWildcardAcceptHeaderUsesJson()
        throws Exception
    {
        var response = connector.getResponse("""
            GET /test/wildcard HTTP/1.1
            Host: localhost
            Accept: text/html,application/xhtml+xml,*/*;q=0.8
            
            """);

        assertThat(response).contains(
            "HTTP/1.1 200 OK",
            "Content-Type: application/json; charset=UTF-8"
        );
    }

    @Test
    void getRequestHonorsQualityValues()
        throws Exception
    {
        var response = connector.getResponse("""
            GET /test/quality HTTP/1.1
            Host: localhost
            Accept: application/json;q=0.5, text/markdown
            
            """);

        assertThat(response).contains("Content-Type: text/markdown; charset=UTF-8");
        assertThat(capturedOutputFormat.get()).isEqualTo(MARKDOWN);
    }

    @Test
    void getRequestCombinesMultipleAcceptHeaders()
        throws Exception
    {
        var response = connector.getResponse("""
            GET /test/multiple HTTP/1.1
            Host: localhost
            Accept: image/png
            Accept: text/markdown
            
            """);

        assertThat(response).contains("Content-Type: text/markdown; charset=UTF-8");
    }

    @Test
    void getRequestWithExplicitFormatParameterOverridesAcceptHeader()
        throws Exception
    {
        var response = connector.getResponse("""
            GET /test/explicit?_format=Markdown HTTP/1.1
            Host: localhost
            Accept: application/json
            
            """);

        assertThat(response).contains("Content-Type: text/markdown; charset=UTF-8");
        assertThat(capturedOutputFormat.get()).isEqualTo(MARKDOWN);
    }

    @Test
    void formatAddedAfterStartupIsNegotiated()
        throws Exception
    {
        var csv = new OutputFormat("csv", "text/csv");
        availableFormats.set(Set.of(JSON, MARKDOWN, csv));

        var response = connector.getResponse("""
            GET /test/dynamic HTTP/1.1
            Host: localhost
            Accept: text/csv
            
            """);

        assertThat(response).contains(
            "HTTP/1.1 200 OK",
            "Content-Type: text/csv; charset=UTF-8",
            "processed:dynamic"
        );
        assertThat(capturedOutputFormat.get()).isEqualTo(csv);
    }

    @Test
    void postRequestIsRejected()
        throws Exception
    {
        var response = connector.getResponse("""
            POST /test/123 HTTP/1.1
            Host: localhost
            Content-Length: 0
            
            """);

        // When handler returns false and no other handler handles it, Jetty responds with 404 or
        // unhandled
        assertThat(response).doesNotContain("processed:123");
    }

    @Test
    void resultWithEmptyOutputSetsStatusAndContentType()
        throws Exception
    {
        var response = connector.getResponse("""
            GET /test/empty HTTP/1.1
            Host: localhost
            
            """);

        assertThat(response).contains(
            "HTTP/1.1 200 OK",
            "Content-Type: application/json; charset=UTF-8"
        );
    }

    @Test
    void clientErrorResponse()
        throws Exception
    {
        var response = connector.getResponse("""
            GET /test/error HTTP/1.1
            Host: localhost
            
            """);

        assertThat(response).contains(
            "HTTP/1.1 400 Bad Request",
            "Invalid id"
        );
    }
}

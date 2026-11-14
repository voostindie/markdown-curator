package nl.ulso.curator.server;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.Curator;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;

import java.util.Map;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.server.Result.notAcceptable;
import static nl.ulso.curator.server.Result.ok;
import static nl.ulso.curator.server.Result.serverError;
import static nl.ulso.curator.vault.Document.EMPTY_DOCUMENT;

/// Executes a query against a curator and outputs the result.
///
/// Any query that is available can be executed through here. The configuration for the query
/// that is normally configured in a document can be passed as query parameters instead.
///
/// The output format is negotiated against the formats the curator supports at the time of the
/// request, which includes the formats of its active extensions; JSON is the default. If the
/// selected format doesn't support the result of the query, the result is in Markdown instead.
@Singleton
final class QueryRequestProcessor
    extends CuratorRequestProcessorTemplate
{
    @Inject
    QueryRequestProcessor(Map<String, Curator> curators)
    {
        super(curators);
    }

    @Override
    protected String subPathTemplate()
    {
        return "/queries/{query}";
    }

    @Override
    protected Result process(
        Curator curator,
        Dictionary pathParameters,
        Dictionary queryParameters,
        ContentNegotiation contentNegotiation)
    {
        var availableFormats = curator.outputFormats();
        var outputFormat = contentNegotiation.select(availableFormats, JSON);
        if (outputFormat.isEmpty())
        {
            return notAcceptable(contentNegotiation.describeFailure(availableFormats));
        }
        try
        {
            var query = curator.query(pathParameters.string("query", "help"));
            var definition = createQueryDefinition(query, queryParameters);
            var result = query.run(definition);
            var formatted = curator.formatResult(result, outputFormat.get());
            return ok(formatted.outputFormat(), formatted.output());
        }
        catch (RuntimeException e)
        {
            return serverError(e.getMessage());
        }
    }

    private QueryDefinition createQueryDefinition(Query query, Dictionary configuration)
    {
        return new QueryDefinition()
        {
            @Override
            public String queryName()
            {
                return query.name();
            }

            @Override
            public Dictionary configuration()
            {
                return configuration;
            }

            @Override
            public Document document()
            {
                return EMPTY_DOCUMENT;
            }
        };
    }
}

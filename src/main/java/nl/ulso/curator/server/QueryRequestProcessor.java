package nl.ulso.curator.server;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.Curator;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;

import java.util.Map;

import static nl.ulso.curator.server.Result.ok;
import static nl.ulso.curator.server.Result.serverError;
import static nl.ulso.curator.vault.Document.EMPTY_DOCUMENT;

/// Executes a query against a curator and outputs the result.
///
/// Any query that is available can be executed through here. The configuration for the query
/// that is normally configured in a document can be passed as query parameters instead.
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
        OutputFormat outputFormat)
    {
        var query = curator.query(pathParameters.string("query", "help"));
        var definition = createQueryDefinition(query, queryParameters);
        try
        {
            var result = query.run(definition);
            if (!curator.supports(result, outputFormat))
            {
                return serverError("Unsupported output format for this query.");
            }
            return ok(outputFormat, curator.format(result, outputFormat));
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

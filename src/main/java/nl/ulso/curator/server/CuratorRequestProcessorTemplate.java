package nl.ulso.curator.server;

import nl.ulso.curator.Curator;
import nl.ulso.dictionary.Dictionary;

import java.util.*;

import static nl.ulso.curator.server.Result.clientError;

/// Base class for request processors that act on a [Curator].
public abstract class CuratorRequestProcessorTemplate
    implements RequestProcessor
{
    private static final String CURATOR_PARAMETER = "curator";
    private static final String CURATOR_PREFIX = "/curators/{" + CURATOR_PARAMETER + "}";

    private final Map<String, Curator> curators;

    CuratorRequestProcessorTemplate(Map<String, Curator> curators)
    {
        var map = HashMap.<String, Curator>newHashMap(curators.size());
        curators.forEach((key, value) -> map.put(key.toLowerCase(), value));
        this.curators = Collections.unmodifiableMap(map);
    }

    @Override
    public final String pathTemplate()
    {
        return CURATOR_PREFIX + subPathTemplate();
    }

    /// @return sub-path template for this request handler, e.g. `/queries/{query}; the subpath is
    /// put after the curator path.
    protected abstract String subPathTemplate();

    @Override
    public final Result process(
        Dictionary pathParameters,
        Dictionary queryParameters,
        ContentNegotiation contentNegotiation)
    {
        var curatorName = pathParameters.string(CURATOR_PARAMETER, "").toLowerCase();
        if (curatorName.isBlank())
        {
            return clientError("Curator name is blank in the path parameters.");
        }
        var curator = curators.get(curatorName);
        if (curator == null)
        {
            return clientError("Curator not found: " + curatorName);
        }
        return process(curator, pathParameters, queryParameters, contentNegotiation);
    }

    /// Processes the request for a specific curator. Content negotiation happens here, after the
    /// curator is known, because the output formats that are available differ per curator: they
    /// depend on the extensions it has loaded.
    protected abstract Result process(
        Curator curator,
        Dictionary pathParameters,
        Dictionary queryParameters,
        ContentNegotiation contentNegotiation);
}

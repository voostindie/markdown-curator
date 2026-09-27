package nl.ulso.curator.server;

import nl.ulso.curator.query.OutputFormat;
import nl.ulso.dictionary.Dictionary;

import java.util.Optional;

/// Simple abstraction for handling GET requests.
///
/// Unlike in a traditional web server, none of the request details - headers, URI, and so on - are
/// made available through this interface. These are all handled by the underlying server
/// implementation. All an implementation can do is generate a [String] output in the requested
/// [OutputFormat], based on the incoming path and query parameters.
public interface RequestProcessor
{
    /// @return path template for this request handler, e.g. `/queries/{query}`. Each of the path
    /// parameters is made available to the [#process] method.
    String pathTemplate();

    /// Process the request, looking only at path and query parameters, and return a result in the
    /// requested [OutputFormat]; if no result can be generated, return an empty [Optional].
    Result process(
        Dictionary pathParameters,
        Dictionary queryParameters,
        OutputFormat requestedOutputFormat);
}

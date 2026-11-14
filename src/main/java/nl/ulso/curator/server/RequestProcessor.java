package nl.ulso.curator.server;

import nl.ulso.dictionary.Dictionary;

/// Simple abstraction for handling GET requests.
///
/// Unlike in a traditional web server, none of the request details - headers, URI, and so on - are
/// made available through this interface. These are all handled by the underlying server
/// implementation. All an implementation can do is generate a [String] output based on the incoming
/// path and query parameters, in the output format it selects through the [ContentNegotiation].
///
/// Selecting the output format is up to the implementation, because only the implementation knows
/// which formats it can produce: these can differ per curator, and can change at runtime.
public interface RequestProcessor
{
    /// @return path template for this request handler, e.g. `/queries/{query}`. Each of the path
    /// parameters is made available to the [#process] method.
    String pathTemplate();

    /// Process the request, looking only at path and query parameters, and return a result in the
    /// output format selected through the content negotiation. If none of the requested formats
    /// can be produced, return [Result#notAcceptable(String)].
    Result process(
        Dictionary pathParameters,
        Dictionary queryParameters,
        ContentNegotiation contentNegotiation);
}

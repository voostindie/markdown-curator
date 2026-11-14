package nl.ulso.curator.server;

import nl.ulso.dictionary.Dictionary;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.MDC;

import static nl.ulso.curator.server.ContentNegotiation.FORMAT_PARAMETER;
import static nl.ulso.dictionary.Dictionary.mapDictionary;
import static org.eclipse.jetty.server.Request.extractQueryParameters;
import static org.slf4j.LoggerFactory.getLogger;

/// Handles GET requests with Jetty.
///
/// Wraps a [RequestProcessor] and executes its [RequestProcessor#process] method. The handler
/// doesn't select the output format itself: it collects what the client asks for in a
/// [ContentNegotiation], and leaves the selection to the processor, which knows the formats that
/// are available at that moment.
final class JettyRequestHandler
    extends Handler.Abstract.NonBlocking
{
    private static final Logger LOGGER = getLogger(JettyRequestHandler.class);
    private static final String CHARSET_UTF_8 = "; charset=UTF-8";

    private final RequestProcessor request;

    JettyRequestHandler(RequestProcessor request)
    {
        this.request = request;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback)
    {
        MDC.put("curator", "HTTP");
        if (!HttpMethod.GET.is(request.getMethod()))
        {
            LOGGER.error("Unsupported method: {}", request.getMethod());
            return false;
        }
        LOGGER.info("Handling request: '{}'.", request.getHttpURI().getPath());
        var pathParameters = resolvePathParameters(request);
        var queryParameters = resolveQueryParameters(request);
        var contentNegotiation = resolveContentNegotiation(request, queryParameters);
        var result = this.request.process(pathParameters, queryParameters, contentNegotiation);
        response.setStatus(result.status().httpStatusCode());
        result.output().ifPresent(
            output ->
            {
                response.getHeaders()
                    .put(HttpHeader.CONTENT_TYPE, result.outputFormat().mimeType() + CHARSET_UTF_8);
                Content.Sink.write(response, true, output, callback);
            }
        );
        return true;
    }

    /// Collects the output formats the client asks for: by name through the `_format` query
    /// parameter, or by MIME type through the `Accept` header. All values of the header are taken
    /// into account, including quality values.
    private ContentNegotiation resolveContentNegotiation(
        Request request, Dictionary queryParameters)
    {
        var acceptValues = request.getHeaders().getValuesList(HttpHeader.ACCEPT);
        var accept = acceptValues.isEmpty() ? null : String.join(",", acceptValues);
        return ContentNegotiation.of(accept, queryParameters.string(FORMAT_PARAMETER, null));
    }

    private Dictionary resolvePathParameters(Request request)
    {
        var pathSpec = (UriTemplatePathSpec) request.getAttribute(PathSpec.class.getName());
        var parameters = pathSpec.getPathParams(request.getHttpURI().getPath());
        return mapDictionary(parameters);
    }

    private Dictionary resolveQueryParameters(Request request)
    {
        return new JettyFieldsDictionary(extractQueryParameters(request));
    }
}

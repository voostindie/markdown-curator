package nl.ulso.curator.server;

import nl.ulso.curator.query.OutputFormat;
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

import java.util.Set;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.dictionary.Dictionary.mapDictionary;
import static org.eclipse.jetty.server.Request.extractQueryParameters;
import static org.slf4j.LoggerFactory.getLogger;

/// Handles GET requests with Jetty.
///
/// Wraps a [RequestProcessor] and executes its [RequestProcessor#process] method.
final class JettyRequestHandler
    extends Handler.Abstract.NonBlocking
{
    private static final Logger LOGGER = getLogger(JettyRequestHandler.class);
    private static final String CHARSET_UTF_8 = "; charset=UTF-8";

    private final RequestProcessor request;
    private final Set<OutputFormat> availableOutputFormats;

    JettyRequestHandler(RequestProcessor request, Set<OutputFormat> availableOutputFormats)
    {
        this.request = request;
        this.availableOutputFormats = availableOutputFormats;
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
        var requestedOutputFormat = resolveOutputFormat(request);
        var result = this.request.process(pathParameters, queryParameters, requestedOutputFormat);
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

    /// Rudimentary form of content negotiation: JSON is the default, but this can be overridden by
    /// matching the mime types in the Accept header on the mime types of the available output
    /// formats.
    ///
    /// Note that the requested output format is not guaranteed to be used in the response:
    /// [RequestProcessor]s might ignore the requested output format.
    private OutputFormat resolveOutputFormat(Request request)
    {
        var header = request.getHeaders().get(HttpHeader.ACCEPT);
        if (header == null)
        {
            return JSON;
        }
        return availableOutputFormats.stream()
            .filter(outputFormat -> header.startsWith(outputFormat.mimeType()))
            .findFirst()
            .orElse(JSON);
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

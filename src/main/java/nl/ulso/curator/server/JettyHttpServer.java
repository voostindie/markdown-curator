package nl.ulso.curator.server;

import jakarta.inject.Inject;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;

import java.util.Set;

import static org.eclipse.jetty.util.VirtualThreads.getDefaultVirtualThreadsExecutor;
import static org.slf4j.LoggerFactory.getLogger;

public class JettyHttpServer
    implements HttpServer
{
    private static final Logger LOGGER = getLogger(JettyHttpServer.class);
    private static final String SERVER_HOST = "localhost";

    private final Server server;

    @Inject
    JettyHttpServer(Set<RequestProcessor> requestProcessors, int serverPort)
    {
        this.server = createServer(serverPort);
        configureServer(requestProcessors);
    }

    private Server createServer(int serverPort)
    {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("HTTP Server");
        threadPool.setVirtualThreadsExecutor(getDefaultVirtualThreadsExecutor());
        var httpServer = new Server(threadPool);
        var configuration = new HttpConfiguration();
        configuration.setSendServerVersion(false);
        var connectionFactory = new HttpConnectionFactory(configuration);
        var connector = new ServerConnector(httpServer, connectionFactory);
        connector.setPort(serverPort);
        connector.setHost(SERVER_HOST);
        httpServer.addConnector(connector);
        return httpServer;
    }

    /// Output formats are not fixed at startup: request processors negotiate them per request, so
    /// that formats introduced by extensions that are loaded later are served as well.
    private void configureServer(Set<RequestProcessor> requestProcessors)
    {
        var handler = new PathMappingsHandler();
        requestProcessors.forEach(processor ->
            handler.addMapping(
                new UriTemplatePathSpec(processor.pathTemplate()),
                new JettyRequestHandler(processor)
            ));
        server.setHandler(handler);

    }

    @Override
    public void start()
    {
        var port = ((NetworkConnector) server.getConnectors()[0]).getPort();
        try
        {
            server.start();
            LOGGER.info("Started HTTP server on port {}.", port);
        }
        catch (Exception _)
        {
            LOGGER.error(
                "Couldn't start HTTP server on port {}. The HTTP server is disabled.",
                port
            );
        }
    }
}

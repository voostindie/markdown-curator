package nl.ulso.curator.server;

import nl.ulso.curator.query.OutputFormat;
import nl.ulso.curator.vault.VaultStub;
import nl.ulso.dictionary.Dictionary;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Set;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration-test")
class JettyHttpServerTest
{
    @Test
    void instantiateAndStartServer()
        throws Exception
    {
        var curator1 =
            new CuratorStub(new VaultStub(), Map.of(), Set.of(JSON));
        var curator2 =
            new CuratorStub(new VaultStub(), Map.of(), Set.of(MARKDOWN));

        var dummyProcessor = new RequestProcessor()
        {
            @Override
            public String pathTemplate()
            {
                return "/dummy";
            }

            @Override
            public Result process(
                Dictionary pathParameters,
                Dictionary queryParameters,
                OutputFormat requestedOutputFormat)
            {
                return Result.ok(JSON, "{}");
            }
        };

        var httpServer = new JettyHttpServer(
            Map.of("curator1", curator1, "curator2", curator2),
            Set.of(dummyProcessor),
            findRandomOpenPortOnAllLocalInterfaces()
        );

        Server jettyServer = getUnderlyingServer(httpServer);
        try
        {
            httpServer.start();
            assertThat(jettyServer.isStarted()).isTrue();
        }
        finally
        {
            jettyServer.stop();
        }
    }

    @Test
    void serverStartHandlesPortConflictGracefully()
        throws Exception
    {
        var port = findRandomOpenPortOnAllLocalInterfaces();
        var server1 = new JettyHttpServer(Map.of(), Set.of(), port);
        var server2 = new JettyHttpServer(Map.of(), Set.of(), port);
        Server jettyServer1 = getUnderlyingServer(server1);
        Server jettyServer2 = getUnderlyingServer(server2);

        try
        {
            server1.start();
            assertThat(jettyServer1.isStarted()).isTrue();

            // Starting server2 on the same port should catch the BindException and fail gracefully
            server2.start();
            assertThat(jettyServer2.isStarted()).isFalse();
        }
        finally
        {
            if (jettyServer1.isStarted())
            {
                jettyServer1.stop();
            }
            if (jettyServer2.isStarted())
            {
                jettyServer2.stop();
            }
        }
    }

    @Test
    void daggerHttpServerFactoryCreatesHttpServerWithAllProcessors()
        throws Exception
    {
        var curator = new CuratorStub();
        HttpServerFactory factory = DaggerHttpServerFactory.builder()
            .curators(Map.of("main", curator))
            .serverPort(findRandomOpenPortOnAllLocalInterfaces())
            .build();
        assertThat(factory).isNotNull();
        var httpServer = factory.httpServer();
        assertThat(httpServer).isInstanceOf(JettyHttpServer.class);
    }

    private Server getUnderlyingServer(JettyHttpServer httpServer)
        throws Exception
    {
        Field serverField = JettyHttpServer.class.getDeclaredField("server");
        serverField.setAccessible(true);
        return (Server) serverField.get(httpServer);
    }

    private int findRandomOpenPortOnAllLocalInterfaces()
        throws IOException
    {
        try (ServerSocket socket = new ServerSocket(0))
        {
            return socket.getLocalPort();
        }
    }
}

package nl.ulso.curator;

import nl.ulso.curator.server.DaggerHttpServerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.helpers.Reporter;

import java.io.IOException;
import java.util.*;
import java.util.ServiceLoader.Provider;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Integer.parseInt;
import static java.lang.System.setProperty;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.callable;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static nl.ulso.curator.RunMode.DAEMON;
import static nl.ulso.curator.RunMode.ONCE;
import static org.slf4j.LoggerFactory.getLogger;

/// Main application: sets up all curators and runs them; each curator runs in its own thread.
///
/// Curators are discovered through the Java Service Loader mechanism; see [CuratorFactory] for more
/// information.
///
/// If a single curator cannot be instantiated, its thread is basically dead; it won't work. The
/// other curators (if any) will still work, though.
public class Application
{
    private static final Logger LOGGER;

    static
    {
        // Make sure we disable SLF4J's useless logging before it initializes itself.
        // Because the application's main method is in this class, this is where and how that
        // needs to happen.
        setProperty(Reporter.SLF4J_INTERNAL_VERBOSITY_KEY, "ERROR");
        LOGGER = getLogger(Application.class);
    }

    private static final String UNKNOWN_VERSION = "<UNKNOWN>";
    private static final int DEFAULT_SERVER_PORT = 7767; // MC

    private final PidManager pidManager;
    private final boolean enableHttpServer;
    private final int serverPort;

    Application(PidManager pidManager, boolean enableHttpServer)
    {
        this(pidManager, enableHttpServer, DEFAULT_SERVER_PORT);
    }

    Application(PidManager pidManager, boolean enableHttpServer, int serverPort)
    {
        this.pidManager = pidManager;
        this.enableHttpServer = enableHttpServer;
        this.serverPort = serverPort;
    }

    static void main(String[] args)
    {
        RunMode.set(DAEMON);
        var curators = new HashSet<String>();
        var enableHttpServer = false;
        var serverPort = DEFAULT_SERVER_PORT;
        for (String arg : args)
        {
            if (arg.contentEquals("--once") || arg.contentEquals("-1"))
            {
                RunMode.set(ONCE);
                continue;
            }
            if (arg.startsWith("--server") || arg.startsWith("-s"))
            {
                if (RunMode.get() == ONCE)
                {
                    LOGGER.warn("Server mode is not available in single-run mode.");
                }
                else
                {
                    enableHttpServer = true;
                    var index = arg.indexOf(':');
                    if (index != -1 && index < arg.length() - 1)
                    {
                        try
                        {
                            serverPort = parseInt(arg.substring(index + 1));
                            if (serverPort < 1024)
                            {
                                LOGGER.warn(
                                    "Invalid port specified. Port numbers below 1024 are " +
                                    "reserved. Falling back on default port.");
                                serverPort = DEFAULT_SERVER_PORT;
                            }
                        }
                        catch (NumberFormatException e)
                        {
                            if (LOGGER.isErrorEnabled())
                            {
                                LOGGER.warn(
                                    "Invalid port specified. Using default port instead: {}.",
                                    DEFAULT_SERVER_PORT
                                );
                            }
                        }
                    }
                }
                continue;
            }
            curators.add(arg.toLowerCase());
        }
        new Application(new DefaultPidManager(), enableHttpServer, serverPort).run(curators);
    }

    void run(Set<String> selectedCuratorNames)
    {
        var factories = ServiceLoader.load(CuratorFactory.class).stream()
            .map(Provider::get)
            .toList();
        if (factories.isEmpty())
        {
            LOGGER.error("No curators are available in the system. Nothing to do!");
            return;
        }
        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info("Markdown Curator {}", resolveVersion());
            LOGGER.info("Press Ctrl+C to stop");
            LOGGER.info("-".repeat(76));
        }
        if (!selectedCuratorNames.isEmpty())
        {
            factories = factories.stream()
                .filter(f -> selectedCuratorNames.contains(f.name().toLowerCase()))
                .toList();
            if (factories.isEmpty())
            {
                LOGGER.error(
                    "No curators are available in the system. Nothing to do! Filter: {}",
                    selectedCuratorNames
                );
                return;
            }
        }
        if (pidManager.anyPidExists(factories))
        {
            LOGGER.error("Another Markdown Curator is running for the same vault(s). Exiting.");
            return;
        }
        var curators = createCurators(factories);
        if (enableHttpServer)
        {
            runEmbeddedHttpServer(curators);
        }
        runCuratorsInParallel(curators);
    }

    String resolveVersion()
    {
        try (var inputStream = Application.class.getClassLoader()
            .getResourceAsStream("markdown-curator.properties"))
        {
            var properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty("version", UNKNOWN_VERSION);
        }
        catch (IOException e)
        {
            LOGGER.warn("Couldn't read properties file from classpath", e);
            return UNKNOWN_VERSION;
        }
    }

    /// Invokes each factory, creating a curator for each one.
    ///
    /// This implementation runs in parallel for each factory, because initializing a curator might
    /// be a long-running operation, depending on the size of the curator: at startup all in the
    /// vault are pulled into memory and processed.
    ///
    /// The size of the map returned might not be equal to the size of the list of factories: if a
    /// curator could not be created, it will not be included in the map. Possible reasons:
    ///
    /// - A Markdown curator is already running for the specific curator (a PID is present).
    /// - Curator initialization throws an exception.
    ///
    /// @return A map of curator names to curators that were successfully created.
    Map<String, Curator> createCurators(List<CuratorFactory> factories)
    {
        Map<String, Curator> result = new ConcurrentHashMap<>(factories.size());
        try (var executor = newVirtualThreadPerTaskExecutor())
        {
            var curators = factories.stream().map(factory -> callable(() ->
            {
                var curatorName = factory.name();
                initializeThreadContext(curatorName);
                if (!pidManager.createPidFor(factory))
                {
                    LOGGER.error("Failed to create PID for curator '{}'. It will not be run.",
                        curatorName
                    );
                    return;
                }
                LOGGER.debug("Instantiating curator: '{}'.", curatorName);
                Curator curator = null;
                try
                {
                    curator = factory.createCurator();
                }
                catch (Exception e)
                {
                    LOGGER.error("Could not create curator '{}'.", curatorName, e);
                }
                if (curator != null)
                {
                    result.put(curatorName, curator);
                }
            })).toList();
            executor.invokeAll(curators);
            return Map.copyOf(result);
        }
        catch (InterruptedException _)
        {
            currentThread().interrupt();
            return emptyMap();
        }
    }

    /// Runs the embedded HTTP server for the given curators. This method does not block. The server
    /// is started in its own thread.
    void runEmbeddedHttpServer(Map<String, Curator> curators)
    {
        var httpServer = DaggerHttpServerFactory
            .builder()
            .serverPort(serverPort)
            .curators(curators)
            .build()
            .httpServer();
        httpServer.start();
    }

    void runCuratorsInParallel(Map<String, Curator> curators)
    {
        try (var executor = newVirtualThreadPerTaskExecutor())
        {
            var tasks = curators.entrySet().stream().map(entry -> callable(() ->
            {
                var curatorName = entry.getKey();
                var curator = entry.getValue();
                initializeThreadContext(curatorName);
                try
                {
                    curator.run(RunMode.get());
                }
                catch (Exception e)
                {
                    LOGGER.error("Curator '{}' errored out. It's non-functional from now on.",
                        curatorName, e
                    );
                }
            })).toList();
            executor.invokeAll(tasks);
        }
        catch (InterruptedException _)
        {
            currentThread().interrupt();
        }
    }

    private void initializeThreadContext(String curatorName)
    {
        currentThread().setName(curatorName);
        MDC.put("curator", curatorName);
    }
}

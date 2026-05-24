package nl.ulso.curator;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.helpers.Reporter;

import java.io.IOException;
import java.util.*;
import java.util.ServiceLoader.Provider;

import static java.lang.System.setProperty;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.callable;
import static java.util.concurrent.Executors.newFixedThreadPool;
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
        // Because the application's main method is in this class, this is how that needs to happen.
        setProperty(Reporter.SLF4J_INTERNAL_VERBOSITY_KEY, "ERROR");
        LOGGER = getLogger(Application.class);
    }

    private static final String UNKNOWN_VERSION = "<UNKNOWN>";

    private final PidManager pidManager;

    Application(PidManager pidManager)
    {
        this.pidManager = pidManager;
    }

    static void main(String[] args)
    {
        RunMode.set(DAEMON);
        var curators = new HashSet<String>();
        for (String arg : args)
        {
            if (arg.contentEquals("--once") || arg.contentEquals("-1"))
            {
                RunMode.set(ONCE);
            }
            else
            {
                curators.add(arg.toLowerCase());
            }
        }
        new Application(new DefaultPidManager()).run(curators);
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
                LOGGER.error("No curators are available in the system. Nothing to do! Filter: {}",
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
        runCuratorsInSeparateThreads(factories);
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

    void runCuratorsInSeparateThreads(List<CuratorFactory> factories)
    {
        try (var executor = newFixedThreadPool(factories.size()))
        {
            var curators = factories.stream().map(factory -> callable(() ->
            {
                var name = factory.name();
                currentThread().setName(name);
                MDC.put("curator", name);
                if (!pidManager.createPidFor(factory))
                {
                    LOGGER.error("Failed to create PID for curator '{}'. It will not be run.",
                        name
                    );
                    return;
                }
                LOGGER.debug("Instantiating curator: {}", name);
                Curator curator = null;
                try
                {
                    curator = factory.createCurator();
                }
                catch (Exception e)
                {
                    LOGGER.error("Could not create curator '{}'. It will not be run.", name, e);
                }
                if (curator != null)
                {
                    try
                    {
                        curator.run(RunMode.get());
                    }
                    catch (Exception e)
                    {
                        LOGGER.error("Curator '{}' errored out. It's non-functional from now on.",
                            name, e
                        );
                    }
                }
            })).toList();
            executor.invokeAll(curators);
        }
        catch (InterruptedException _)
        {
            currentThread().interrupt();
        }
    }
}

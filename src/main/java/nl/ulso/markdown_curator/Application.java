package nl.ulso.markdown_curator;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.ServiceLoader.Provider;

import static com.google.inject.Guice.createInjector;
import static java.lang.System.getProperty;
import static java.lang.System.lineSeparator;
import static java.lang.Thread.currentThread;
import static java.nio.file.Files.writeString;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.util.concurrent.Executors.callable;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Main application: sets up all curator and runs them; each curator runs in its own thread.
 * <p/>
 * If a single curator cannot be instantiated, its thread is basically dead; it won't work. The
 * other curators (if any) will still work though.
 */
public class Application
{
    private static final Logger LOGGER = getLogger(Application.class);
    private static final Path
            DEFAULT_PID = Path.of(getProperty("java.io.tmpdir"), "markdown-curator.pid");

    static final String UNKNOWN_VERSION = "<UNKNOWN>";
    private final Path pid;

    enum RunMode
    {
        ONCE,
        DAEMON
    }

    Application(Path pid)
    {
        this.pid = pid;
    }

    public static void main(String[] args)
    {
        var runMode = RunMode.DAEMON;
        if (args.length > 0 && (args[0].contentEquals("--once") || args[0].contentEquals("-1")))
        {
            runMode = RunMode.ONCE;
        }
        new Application(DEFAULT_PID).run(runMode);
    }

    void run(RunMode runMode)
    {
        if (!ensureNewPidFile())
        {
            LOGGER.error("Couldn't write PID. Another Markdown Curator is running. Exiting.");
            return;
        }
        var modules = ServiceLoader.load(CuratorModule.class).stream().map(Provider::get).toList();
        if (modules.isEmpty())
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
        runCuratorsInSeparateThreads(modules, runMode);
    }

    boolean ensureNewPidFile()
    {
        try
        {
            writeString(pid, ProcessHandle.current().pid() + lineSeparator(), CREATE_NEW);
        }
        catch (IOException e)
        {
            return false;
        }
        pid.toFile().deleteOnExit();
        return true;
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
            LOGGER.warn("Can't read properties file from classpath", e);
            return UNKNOWN_VERSION;
        }
    }

    void runCuratorsInSeparateThreads(List<CuratorModule> modules, RunMode runMode)
    {
        try (var executor = newFixedThreadPool(modules.size()))
        {
            var curators = modules.stream().map(module -> callable(() ->
            {
                var name = module.name();
                currentThread().setName(name);
                LOGGER.debug("Instantiating curator: {}", name);
                Curator curator = null;
                try
                {
                    curator = createInjector(module).getInstance(Curator.class);
                }
                catch (Exception e)
                {
                    LOGGER.error("Could not create curator '{}'. It will not be run.", name, e);
                }
                if (curator != null)
                {
                    try
                    {
                        switch (runMode)
                        {
                            case DAEMON -> curator.run();
                            case ONCE -> curator.runOnce();
                        }
                    }
                    catch (Exception e)
                    {
                        LOGGER.error("Curator '{}' errored out. It's non-functional from now on.",
                                name, e);
                    }

                }
            })).toList();
            executor.invokeAll(curators);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}

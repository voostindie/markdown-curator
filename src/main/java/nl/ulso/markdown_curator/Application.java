package nl.ulso.markdown_curator;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;

import static java.lang.System.getProperty;
import static java.lang.System.lineSeparator;
import static java.nio.file.Files.writeString;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
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
    private static final Path PID = Path.of(getProperty("java.io.tmpdir"), "markdown-curator.pid");

    static final String UNKNOWN_VERSION = "<UNKNOWN>";

    public static void main(String[] args)
    {
        ensureNewPidFile();
        var providers = ServiceLoader.load(CuratorFactory.class).stream().toList();
        if (providers.isEmpty())
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
        var executor = Executors.newFixedThreadPool(providers.size());
        providers.forEach(provider -> executor.submit(Executors.callable(() -> {
            var factory = provider.get();
            var name = factory.name();
            Thread.currentThread().setName(name);
            LOGGER.debug("Instantiating curator: {}", name);
            try
            {
                factory.createCurator().run();
            }
            catch (Exception e)
            {
                LOGGER.error("Curator '{}' errored out. It's non-functional from now on.", name, e);
            }
        })));
        executor.shutdown();
    }

    private static void ensureNewPidFile()
    {
        try
        {
            writeString(PID, ProcessHandle.current().pid() + lineSeparator(), CREATE_NEW);
        }
        catch (IOException e)
        {
            LOGGER.error(
                    "Couldn't write PID. Another Markdown Curator is probably running. Exiting.");
            System.exit(-1);
        }
        PID.toFile().deleteOnExit();
    }

    static String resolveVersion()
    {
        try (var inputStream =
                     Application.class.getClassLoader()
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
}

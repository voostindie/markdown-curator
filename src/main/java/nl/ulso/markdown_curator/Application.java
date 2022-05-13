package nl.ulso.markdown_curator;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;

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
    static final String UNKNOWN_VERSION = "<UNKNOWN>";

    public static void main(String[] args)
    {
        LOGGER.info("Markdown Curator {}", resolveVersion());
        LOGGER.info("Press Ctrl+C to stop");
        LOGGER.info("-".repeat(76));
        var providers = ServiceLoader.load(CuratorFactory.class).stream().toList();
        if (providers.isEmpty())
        {
            LOGGER.error("No curators are available in the system. Nothing to do!");
            return;
        }
        var executor = Executors.newFixedThreadPool(providers.size());
        providers.forEach(provider -> executor.submit(Executors.callable(() -> {
            var factory = provider.get();
            Thread.currentThread().setName(factory.name());
            LOGGER.debug("Instantiating curator: {}", factory.name());
            var curator = factory.createCurator();
            curator.run();
        })));
        executor.shutdown();
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

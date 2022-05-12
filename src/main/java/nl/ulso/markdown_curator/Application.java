package nl.ulso.markdown_curator;

import org.slf4j.Logger;

import java.io.IOException;
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

    public static void main(String[] args)
    {
        LOGGER.info("Markdown Curator {}", version());
        LOGGER.info("Press Ctrl+C to stop");
        LOGGER.info("-".repeat(76));
        ServiceLoader<CuratorFactory> loader = ServiceLoader.load(CuratorFactory.class);
        var factories = loader.stream().toList();
        if (factories.isEmpty())
        {
            LOGGER.error("No curators are available in the system. Nothing to do!");
            return;
        }
        var executor = Executors.newFixedThreadPool(factories.size());
        loader.forEach(factory -> executor.submit(Executors.callable(() -> {
            try
            {
                Thread.currentThread().setName(factory.name());
                LOGGER.debug("Instantiating curator: {}", factory.name());
                var curator = factory.createCurator();
                curator.run();
            }
            catch (IOException e)
            {
                LOGGER.error("Error in configuration for {}. Shutting it down",
                        factory.name(), e);
            }
            catch (InterruptedException e)
            {
                LOGGER.error("Got interrupted!", e);
                Thread.currentThread().interrupt();
            }
        })));
        executor.shutdown();
    }

    private static String version()
    {
        return "1.0.0-SNAPSHOT";
    }
}

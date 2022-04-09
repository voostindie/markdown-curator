package nl.ulso.macu;

import nl.ulso.macu.config.personal.PersonalSystem;
import nl.ulso.macu.config.rabobank.RabobankSystem;
import nl.ulso.macu.config.tweevv.TweevvSystem;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Main application: sets up all vaults and watches them for changes, updating the in-memory
 * model as changes come in.
 * <p/>
 * Each vault is managed in its own thread.
 */
public class Application
{
    private static final Logger LOGGER = getLogger(Application.class);

    public static void main(String[] args)
    {
        LOGGER.info("Macu {}", version());
        LOGGER.info("Press Ctrl+C to stop");
        LOGGER.info("--------------------------------------------------");
        var systems = systems();
        var executor = Executors.newFixedThreadPool(systems.size());
        for (Class<? extends System> clazz : systems)
        {
            executor.submit(Executors.callable(() -> {
                try
                {
                    Thread.currentThread().setName(clazz.getSimpleName());
                    LOGGER.debug("Instantiating system: {}", clazz.getSimpleName());
                    var system = clazz.getDeclaredConstructor().newInstance();
                    system.vault().watchForChanges();
                }
                catch (ReflectiveOperationException e)
                {
                    LOGGER.error("Could not instantiate system: {}. Cause: {}"
                            , clazz.getSimpleName(), e);
                    throw new IllegalStateException(
                            "Could not instantiate vault graph class: " + clazz);
                }
                catch (IOException e)
                {
                    LOGGER.error("Error in configuration {}. Shutting it down", clazz, e);
                }
                catch (InterruptedException e)
                {
                    LOGGER.error("Got interrupted!", e);
                    Thread.currentThread().interrupt();
                }
            }));
        }
        executor.shutdown();
    }

    private static Set<Class<? extends System>> systems()
    {
        return Set.of(
                RabobankSystem.class,
                PersonalSystem.class,
                TweevvSystem.class
        );
    }

    private static String version()
    {
        return "1.0.0-SNAPSHOT";
    }
}

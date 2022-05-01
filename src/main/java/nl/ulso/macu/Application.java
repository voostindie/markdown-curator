package nl.ulso.macu;

import nl.ulso.macu.curator.Curator;
import nl.ulso.macu.curator.personal.PersonalNotesCurator;
import nl.ulso.macu.curator.rabobank.RabobankNotesCurator;
import nl.ulso.macu.curator.tweevv.TweevvNotesCurator;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Set;
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
        LOGGER.info("Macu {}", version());
        LOGGER.info("Press Ctrl+C to stop");
        LOGGER.info("--------------------------------------------------");
        var systems = systems();
        var executor = Executors.newFixedThreadPool(systems.size());
        for (Class<? extends Curator> clazz : systems)
        {
            executor.submit(Executors.callable(() -> {
                try
                {
                    Thread.currentThread().setName(clazz.getSimpleName());
                    LOGGER.debug("Instantiating system: {}", clazz.getSimpleName());
                    var system = clazz.getDeclaredConstructor().newInstance();
                    system.run();
                }
                catch (ReflectiveOperationException e)
                {
                    LOGGER.error("Could not instantiate system {}. It will not run!"
                            , clazz.getSimpleName());
                    throw new IllegalStateException(
                            "Could not instantiate system class: " + clazz);
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

    private static Set<Class<? extends Curator>> systems()
    {
        return Set.of(
                RabobankNotesCurator.class,
                PersonalNotesCurator.class,
                TweevvNotesCurator.class
        );
    }

    private static String version()
    {
        return "1.0.0-SNAPSHOT";
    }
}

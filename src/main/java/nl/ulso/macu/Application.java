package nl.ulso.macu;

import nl.ulso.macu.system.System;
import nl.ulso.macu.system.personal.Personal;
import nl.ulso.macu.system.rabobank.Rabobank;
import nl.ulso.macu.system.tweevv.Tweevv;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Main application: sets up all systems and runs them; each system runs in its own thread.
 * <p/>
 * If a single system cannot be instantiated, its thread is basically dead; it won't work. The other
 * systems (if any) will still work though.
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

    private static Set<Class<? extends System>> systems()
    {
        return Set.of(
                Rabobank.class,
                Personal.class,
                Tweevv.class
        );
    }

    private static String version()
    {
        return "1.0.0-SNAPSHOT";
    }
}

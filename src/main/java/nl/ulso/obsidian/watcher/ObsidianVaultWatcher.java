package nl.ulso.obsidian.watcher;

import nl.ulso.obsidian.watcher.config.personal.PersonalVault;
import nl.ulso.obsidian.watcher.config.rabobank.RabobankVault;
import nl.ulso.obsidian.watcher.config.tweevv.TweevvVault;
import nl.ulso.obsidian.watcher.vault.FileSystemVault;
import nl.ulso.obsidian.watcher.vault.Vault;
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
public class ObsidianVaultWatcher
{
    private static final Logger LOGGER = getLogger(ObsidianVaultWatcher.class);

    public static void main(String[] args)
    {
        LOGGER.info("Obsidian Vault Watcher {}", version());
        var vaults = vaultClasses();
        var executor = Executors.newFixedThreadPool(vaults.size());
        for (Class<? extends Vault> vaultClass : vaults)
        {
            executor.submit(Executors.callable(() -> {
                        Vault vault;
                        try
                        {
                            vault = vaultClass.getDeclaredConstructor().newInstance();
                            vault.watchForChanges();
                        }
                        catch (ReflectiveOperationException e)
                        {
                            throw new IllegalStateException(
                                    "Could not instantiate vault class: " + vaultClass);
                        }
                        catch (IOException e)
                        {
                            LOGGER.error("Error in vault {}. Shutting it down", vaultClass, e);
                        }
                        catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                            LOGGER.error("Got interrupted!", e);
                        }
                    })
            );
        }
        LOGGER.info("Press Ctrl+C to stop");
        executor.shutdown();
    }

    private static Set<Class<? extends FileSystemVault>> vaultClasses()
    {
        return Set.of(
                RabobankVault.class,
                TweevvVault.class,
                PersonalVault.class);
    }

    private static String version()
    {
        return "1.0.0-SNAPSHOT";
    }
}

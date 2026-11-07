package nl.ulso.curator.main;

import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionLifecycle;
import org.slf4j.Logger;

import java.util.*;

import static java.util.Collections.unmodifiableSequencedMap;
import static java.util.Collections.unmodifiableSortedMap;
import static org.slf4j.LoggerFactory.getLogger;

/// A set of [Extension]s that are loaded together, as the result of a single configuration.
///
/// All extensions in a generation are created at the same time, providers of services before
/// their consumers, and they are activated, started and stopped at the same time. Within a
/// generation, services never change. A new configuration always results in a new generation.
///
/// Extensions can be dropped from a generation until it is activated, for example because their
/// change processors can't be ordered. Dropping an extension drops all extensions that require
/// its services as well. The generation keeps track of why extensions were dropped.
///
/// A generation is not thread-safe; it's only used from the curator thread.
final class Generation
{
    private static final Logger LOGGER = getLogger(Generation.class);

    static final Generation EMPTY = new Generation(new LinkedHashMap<>(), Map.of(), new TreeMap<>());

    private final SequencedMap<String, Extension> extensions;
    private final Map<String, Set<String>> dependencies;
    private final SortedMap<String, String> failures;
    private boolean started;

    /// @param extensions   the extensions in this generation, in creation order.
    /// @param dependencies for each extension, the extensions whose services it requires.
    /// @param failures     for each extension that couldn't be loaded, the reason.
    Generation(
        SequencedMap<String, Extension> extensions,
        Map<String, Set<String>> dependencies,
        SortedMap<String, String> failures)
    {
        this.extensions = new LinkedHashMap<>(extensions);
        this.dependencies = Map.copyOf(dependencies);
        this.failures = new TreeMap<>(failures);
        this.started = false;
    }

    /// @return the extensions in this generation, in creation order.
    SequencedMap<String, Extension> extensions()
    {
        return unmodifiableSequencedMap(extensions);
    }

    /// @return the reasons why extensions were not loaded, by extension name.
    SortedMap<String, String> failures()
    {
        return unmodifiableSortedMap(failures);
    }

    /// Drops an extension from this generation, as well as all extensions that (indirectly)
    /// require its services. Dropping an extension that is not in the generation does nothing.
    void drop(String name, String reason)
    {
        if (started)
        {
            throw new IllegalStateException("Cannot drop extensions from a started generation");
        }
        if (extensions.remove(name) == null)
        {
            return;
        }
        LOGGER.warn("Dropping extension '{}': {}", name, reason);
        failures.put(name, reason);
        dependencies.entrySet().stream()
            .filter(entry -> entry.getValue().contains(name))
            .map(Map.Entry::getKey)
            .sorted()
            .forEach(dependent -> drop(dependent,
                "Requires services of extension '" + name + "', which was dropped."));
    }

    void start()
    {
        if (started)
        {
            return;
        }
        started = true;
        lifecycles().forEach(lifecycle -> {
            try
            {
                lifecycle.start();
            }
            catch (RuntimeException e)
            {
                LOGGER.error("Failed to start extension component {}.", lifecycle, e);
            }
        });
    }

    void stop()
    {
        if (!started)
        {
            return;
        }
        started = false;
        lifecycles().reversed().forEach(lifecycle -> {
            try
            {
                lifecycle.stop();
            }
            catch (RuntimeException e)
            {
                LOGGER.error("Failed to stop extension component {}.", lifecycle, e);
            }
        });
    }

    /// @return all lifecycles, in the order of creation of their extensions.
    private List<ExtensionLifecycle> lifecycles()
    {
        return extensions.values().stream()
            .flatMap(extension -> extension.lifecycles().stream())
            .toList();
    }
}

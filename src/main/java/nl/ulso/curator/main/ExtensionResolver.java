package nl.ulso.curator.main;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.extension.*;
import nl.ulso.dictionary.Dictionary;
import org.slf4j.Logger;

import java.util.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static org.slf4j.LoggerFactory.getLogger;

/// Turns a configuration - a map of extension names to their settings - into a [Generation] of
/// extensions.
///
/// Extensions that can't be loaded are dropped, with a reason, instead of failing the whole
/// configuration. In order:
///
/// 1. **Describe**: unknown extensions and extensions with unsupported settings are dropped. The
/// [Contribution]s of the others are collected.
/// 2. **Resolve**: extensions that provide a service that is also provided by another extension or
/// by the curator are dropped, as are extensions that require a service nobody provides. This
/// repeats until nothing changes, because dropping an extension removes the services it provides.
/// 3. **Order**: extensions are sorted so that providers of services come before their consumers.
/// Extensions in a dependency cycle are dropped.
/// 4. **Create**: extensions are created in order. If an extension fails to create, or registers
/// different services than it declared, it is dropped, and resolution starts all over again
/// without it: its contributions are gone, and other extensions might depend on it.
@Singleton
final class ExtensionResolver
{
    private static final Logger LOGGER = getLogger(ExtensionResolver.class);

    private final SortedMap<String, ExtensionFactory> factories;
    private final Map<Class<?>, Object> curatorServices;
    private final List<Contribution> curatorContributions;

    @Inject
    ExtensionResolver(
        Map<String, ExtensionFactory> factories,
        Set<ServiceRegistration> curatorServices,
        Set<Contribution> curatorContributions)
    {
        this.factories = new TreeMap<>(factories);
        var services = new HashMap<Class<?>, Object>();
        for (ServiceRegistration registration : curatorServices)
        {
            if (services.putIfAbsent(registration.type(), registration.instance()) != null)
            {
                throw new IllegalStateException("The curator publishes more than one service of " +
                                                "type " + registration.type().getName());
            }
        }
        this.curatorServices = Map.copyOf(services);
        this.curatorContributions = List.copyOf(curatorContributions);
    }

    SortedMap<String, ExtensionFactory> factories()
    {
        return Collections.unmodifiableSortedMap(factories);
    }

    Generation resolve(SortedMap<String, Dictionary> configuration)
    {
        var failures = new TreeMap<String, String>();
        var candidates = describe(configuration, failures);
        while (true)
        {
            resolveServices(candidates, failures);
            var order = order(candidates, failures);
            var generation = create(order, candidates, failures);
            if (generation.isPresent())
            {
                return generation.get();
            }
        }
    }

    private record Candidate(
        String name,
        ExtensionFactory factory,
        Dictionary configuration,
        List<Contribution> contributions)
    {
    }

    private SortedMap<String, Candidate> describe(
        SortedMap<String, Dictionary> configuration, SortedMap<String, String> failures)
    {
        var candidates = new TreeMap<String, Candidate>();
        configuration.forEach((name, settings) -> {
            var factory = factories.get(name);
            if (factory == null)
            {
                fail(failures, name, "Unknown extension. Available: " +
                                     (factories.isEmpty() ? "none" : String.join(", ",
                                         factories.keySet())) + ".");
                return;
            }
            var supported = factory.supportedConfiguration().keySet();
            var unsupported = settings.propertyNames().stream()
                .filter(property -> !supported.contains(property))
                .sorted()
                .toList();
            if (!unsupported.isEmpty())
            {
                fail(failures, name, "Unsupported setting(s): " + String.join(", ", unsupported) +
                                     ". " + (supported.isEmpty()
                                             ? "This extension has no settings."
                                             : "Supported: " +
                                               String.join(", ", new TreeSet<>(supported)) + "."));
                return;
            }
            try
            {
                var contributions = List.copyOf(factory.contributions(settings));
                candidates.put(name, new Candidate(name, factory, settings, contributions));
            }
            catch (RuntimeException e)
            {
                fail(failures, name, "Invalid configuration: " + e.getMessage());
            }
        });
        return candidates;
    }

    private void resolveServices(
        SortedMap<String, Candidate> candidates, SortedMap<String, String> failures)
    {
        var changed = true;
        while (changed)
        {
            changed = false;
            var providers = providersOf(candidates);
            for (var entry : providers.entrySet())
            {
                var type = entry.getKey();
                var names = entry.getValue();
                if (curatorServices.containsKey(type))
                {
                    names.forEach(name -> fail(failures, name, "Provides service " +
                        type.getSimpleName() + ", which the curator already provides."));
                }
                else if (names.size() > 1)
                {
                    names.forEach(name -> fail(failures, name, "Service " +
                        type.getSimpleName() + " is provided by more than one extension: " +
                        String.join(", ", names) + "."));
                }
                else
                {
                    continue;
                }
                names.forEach(candidates::remove);
                changed = true;
            }
            if (changed)
            {
                continue;
            }
            for (var candidate : List.copyOf(candidates.values()))
            {
                var missing = candidate.factory().requires().stream()
                    .filter(type -> !curatorServices.containsKey(type) &&
                                    !providers.containsKey(type))
                    .sorted(Comparator.comparing(Class::getSimpleName))
                    .toList();
                if (!missing.isEmpty())
                {
                    fail(failures, candidate.name(), "Missing required service(s): " +
                        missing.stream()
                            .map(type -> type.getSimpleName() + " (" + hintFor(type, failures) + ")")
                            .collect(joining(", ")) + ".");
                    candidates.remove(candidate.name());
                    changed = true;
                }
            }
        }
    }

    private SortedMap<Class<?>, SortedSet<String>> providersOf(Map<String, Candidate> candidates)
    {
        var providers = new TreeMap<Class<?>, SortedSet<String>>(
            Comparator.comparing(Class::getName));
        candidates.values().forEach(candidate ->
            candidate.factory().provides().forEach(type ->
                providers.computeIfAbsent(type, _ -> new TreeSet<>()).add(candidate.name())));
        return providers;
    }

    /// Explains how a missing service can be made available.
    private String hintFor(Class<?> type, SortedMap<String, String> failures)
    {
        var candidates = factories.entrySet().stream()
            .filter(entry -> entry.getValue().provides().contains(type))
            .map(Map.Entry::getKey)
            .toList();
        if (candidates.isEmpty())
        {
            return "no extension provides it";
        }
        var failed = candidates.stream().filter(failures::containsKey).toList();
        if (!failed.isEmpty())
        {
            return "provided by " + quote(failed) + ", which failed to load";
        }
        return "enable " + quote(candidates);
    }

    private static String quote(List<String> names)
    {
        return names.stream().map(name -> "'" + name + "'").collect(joining(" or "));
    }

    /// Sorts the candidates so that providers of services come before their consumers, dropping
    /// all candidates in a dependency cycle.
    private List<Candidate> order(
        SortedMap<String, Candidate> candidates, SortedMap<String, String> failures)
    {
        var providers = providersOf(candidates);
        var remaining = new ArrayList<>(candidates.values());
        var placed = new LinkedHashSet<String>();
        var progress = true;
        while (progress && !remaining.isEmpty())
        {
            progress = false;
            for (var iterator = remaining.iterator(); iterator.hasNext(); )
            {
                var candidate = iterator.next();
                var dependencies = dependenciesOf(candidate, providers);
                if (placed.containsAll(dependencies))
                {
                    placed.add(candidate.name());
                    iterator.remove();
                    progress = true;
                }
            }
        }
        if (!remaining.isEmpty())
        {
            var names = remaining.stream().map(Candidate::name).toList();
            names.forEach(name -> {
                fail(failures, name, "Circular service dependency between extensions: " +
                                     String.join(", ", names) + ".");
                candidates.remove(name);
            });
        }
        return placed.stream().map(candidates::get).toList();
    }

    private Set<String> dependenciesOf(
        Candidate candidate, SortedMap<Class<?>, SortedSet<String>> providers)
    {
        return candidate.factory().requires().stream()
            .filter(providers::containsKey)
            .flatMap(type -> providers.get(type).stream())
            .filter(name -> !name.equals(candidate.name()))
            .collect(toCollection(TreeSet::new));
    }

    /// Creates all extensions in order. If one fails, it is removed from the candidates and the
    /// result is empty, to signal that resolution must start over.
    private Optional<Generation> create(
        List<Candidate> order,
        SortedMap<String, Candidate> candidates,
        SortedMap<String, String> failures)
    {
        var contributions = new ArrayList<>(curatorContributions);
        order.forEach(candidate -> contributions.addAll(candidate.contributions()));
        var publishedServices = new HashMap<Class<?>, Object>();
        var serviceOwners = new HashMap<Class<?>, String>();
        var extensions = new LinkedHashMap<String, Extension>();
        var dependencies = new HashMap<String, Set<String>>();
        for (var candidate : order)
        {
            var name = candidate.name();
            var factory = candidate.factory();
            var context = new DefaultExtensionContext(
                name, factory.requires(), publishedServices, contributions);
            final Extension extension;
            try
            {
                extension = factory.create(candidate.configuration(), context);
            }
            catch (RuntimeException e)
            {
                // An IllegalArgumentException signals invalid configuration: the message tells
                // the user what to fix, so a stack trace is noise. Anything else is likely a bug.
                if (!(e instanceof IllegalArgumentException))
                {
                    LOGGER.error("Could not create extension '{}'.", name, e);
                }
                fail(failures, name, "Could not create extension: " + e.getMessage());
                candidates.remove(name);
                return Optional.empty();
            }
            var registered = extension.services().stream()
                .map(ServiceRegistration::type)
                .collect(toCollection(HashSet<Class<?>>::new));
            if (registered.size() != extension.services().size() ||
                !registered.equals(factory.provides()))
            {
                fail(failures, name, "Registered services (" + names(registered) +
                                     ") don't match the declared services (" +
                                     names(factory.provides()) + ").");
                candidates.remove(name);
                return Optional.empty();
            }
            extension.services().forEach(registration -> {
                publishedServices.put(registration.type(), registration.instance());
                serviceOwners.put(registration.type(), name);
            });
            dependencies.put(name, factory.requires().stream()
                .map(serviceOwners::get)
                .filter(Objects::nonNull)
                .collect(toCollection(TreeSet::new)));
            extensions.put(name, extension);
        }
        return Optional.of(new Generation(extensions, dependencies, failures));
    }

    private static String names(Collection<Class<?>> types)
    {
        return types.isEmpty()
               ? "none"
               : types.stream().map(Class::getSimpleName).sorted().collect(joining(", "));
    }

    private static void fail(SortedMap<String, String> failures, String name, String reason)
    {
        LOGGER.warn("Not loading extension '{}': {}", name, reason);
        failures.put(name, reason);
    }

    /// The context of a single extension while it is being created.
    private final class DefaultExtensionContext
        implements ExtensionContext
    {
        private final String extensionName;
        private final Set<Class<?>> required;
        private final Map<Class<?>, Object> publishedServices;
        private final List<Contribution> contributions;

        DefaultExtensionContext(
            String extensionName,
            Set<Class<?>> required,
            Map<Class<?>, Object> publishedServices,
            List<Contribution> contributions)
        {
            this.extensionName = extensionName;
            this.required = required;
            this.publishedServices = publishedServices;
            this.contributions = contributions;
        }

        @Override
        public <T> T service(Class<T> type)
        {
            if (!required.contains(type))
            {
                throw new IllegalStateException("Extension '" + extensionName +
                                                "' doesn't declare that it requires service " +
                                                type.getSimpleName() + ".");
            }
            var service = curatorServices.getOrDefault(type, publishedServices.get(type));
            if (service == null)
            {
                // Can't happen: requirements are resolved before creation.
                throw new IllegalStateException("Service not available: " + type.getSimpleName());
            }
            return type.cast(service);
        }

        @Override
        public <T> List<T> contributionsTo(ExtensionPoint<T> extensionPoint)
        {
            return contributions.stream()
                .filter(contribution -> contribution.extensionPoint().equals(extensionPoint))
                .map(contribution -> extensionPoint.type().cast(contribution.value()))
                .toList();
        }
    }
}

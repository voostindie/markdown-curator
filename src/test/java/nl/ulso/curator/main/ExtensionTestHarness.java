package nl.ulso.curator.main;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.Change;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ServiceRegistration;
import nl.ulso.curator.query.DynamicQueryCatalog;
import nl.ulso.curator.query.Query;
import nl.ulso.dictionary.Dictionary;

import java.util.*;
import java.util.stream.Stream;

import static nl.ulso.curator.change.Reset.RESET;
import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static nl.ulso.dictionary.Dictionary.mapDictionary;

/// Test harness for extensions: enables extensions, loads them into the change processor
/// orchestrator, and gives access to what's inside them.
///
/// To use it, define a Dagger component that includes the [ChangeProcessorTestModule] and the
/// modules that register the extensions under test, and expose the harness from it:
///
/// ```java
/// @Singleton
/// @Component(modules = {ChangeProcessorTestModule.class, ProjectExtensionModule.class})
/// interface ProjectTestContext
/// {
///     VaultStub vaultStub();
///     ExtensionTestHarness harness();
/// }
/// ```
///
/// Then, in a test: `context.harness().enable("project").start()`.
@Singleton
public final class ExtensionTestHarness
{
    private final ChangeProcessorOrchestrator orchestrator;
    private final ExtensionRegistry registry;
    private final ExtensionResolver resolver;
    private final DynamicQueryCatalog queryCatalog;
    private final SortedMap<String, Dictionary> configuration;

    @Inject
    ExtensionTestHarness(
        ChangeProcessorOrchestrator orchestrator,
        ExtensionRegistry registry,
        ExtensionResolver resolver,
        DynamicQueryCatalog queryCatalog)
    {
        this.orchestrator = orchestrator;
        this.registry = registry;
        this.resolver = resolver;
        this.queryCatalog = queryCatalog;
        this.configuration = new TreeMap<>();
    }

    public ExtensionTestHarness enable(String extension)
    {
        configuration.put(extension, emptyDictionary());
        return this;
    }

    public ExtensionTestHarness enable(String extension, Map<String, ?> settings)
    {
        configuration.put(extension, mapDictionary(settings));
        return this;
    }

    /// Loads all enabled extensions and runs a [nl.ulso.curator.change.Reset] through the system.
    public Changelog start()
    {
        registry.stage(resolver.resolve(configuration));
        return orchestrator.runFor(List.of(RESET));
    }

    public Changelog runFor(List<Change<?>> changes)
    {
        return orchestrator.runFor(changes);
    }

    public SortedSet<String> activeExtensions()
    {
        return registry.status().active();
    }

    public SortedMap<String, String> failures()
    {
        return registry.status().failures();
    }

    public Query query(String name)
    {
        return queryCatalog.query(name);
    }

    /// Finds the one object of a specific type in the active extensions: a service, change
    /// processor, query, tracker or lifecycle.
    public <T> T find(Class<T> type)
    {
        var matches = registry.activeExtensions().stream()
            .flatMap(ExtensionTestHarness::contentsOf)
            .filter(type::isInstance)
            .distinct()
            .toList();
        if (matches.size() != 1)
        {
            throw new IllegalStateException("Expected exactly one " + type.getSimpleName() +
                                            " in the active extensions, found " + matches.size() +
                                            ". Failures: " + failures());
        }
        return type.cast(matches.getFirst());
    }

    private static Stream<Object> contentsOf(Extension extension)
    {
        return Stream.of(
            extension.services().stream().map(ServiceRegistration::instance),
            extension.changeProcessors().stream(),
            extension.queries().stream(),
            extension.measurementTrackers().stream(),
            extension.lifecycles().stream()
        ).flatMap(stream -> stream.map(Object.class::cast));
    }
}

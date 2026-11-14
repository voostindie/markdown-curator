package nl.ulso.curator.main;

import nl.ulso.curator.change.*;
import nl.ulso.curator.extension.*;
import nl.ulso.curator.main.ChangeProcessorOrchestratorTest.ChangeProcessorStub;
import nl.ulso.curator.main.ChangeProcessorOrchestratorTest.NullStatistics;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.Reset.RESET;
import static nl.ulso.curator.extension.ExtensionPoint.extensionPoint;
import static nl.ulso.curator.extension.ServiceRegistration.service;
import static nl.ulso.curator.main.ExtensionFactoryStub.factory;
import static nl.ulso.curator.main.ExtensionStub.extension;
import static nl.ulso.curator.main.ExtensionTestSupport.newExtensionRegistry;
import static nl.ulso.curator.query.QueryTestModule.createQueryResultFormatterRegistry;
import static nl.ulso.curator.vault.QueryBlockTest.emptyQueryBlock;
import static org.assertj.core.api.Assertions.assertThat;

/// Tests the complete flow of loading and unloading extensions: the [ConfigurationLoader] reads the
/// configuration document, the [ExtensionResolver] creates the extensions, the
/// [DefaultChangeProcessorOrchestrator] activates them.
@ExtendWith(SoftAssertionsExtension.class)
class ExtensionLoadingTest
{
    private static final String CONFIGURATION_DOCUMENT = "config";

    @InjectSoftAssertions
    private SoftAssertions softly;

    private VaultStub vault;
    private List<DocumentRecorder> recorders;
    private DynamicQueryCatalogStub catalog;
    private DynamicQueryResultFormatterRegistry formatterRegistry;
    private Map<String, ExtensionFactory> factories;
    private Set<ServiceRegistration> curatorServices;
    private Set<Contribution> curatorContributions;
    private ExtensionRegistry registry;
    private CuratorSettings settings;
    private DefaultChangeProcessorOrchestrator orchestrator;

    /// Simple service for testing.
    interface Greeter
    {
        String greet();
    }

    /// Value type for the test extension point.
    record Topping(String name)
    {
    }

    /// Extension point for testing.
    private static final ExtensionPoint<Topping> TOPPINGS = extensionPoint("toppings", Topping.class);

    /// Records the lifecycle of an extension.
    static final class LifecycleRecorder
        implements ExtensionLifecycle
    {
        private int starts = 0;
        private int stops = 0;

        @Override
        public void start()
        {
            starts++;
        }

        @Override
        public void stop()
        {
            stops++;
        }
    }

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        vault.addDocumentInPath("a/doc1", "");
        vault.addDocumentInPath("a/doc2", "");
        vault.addDocumentInPath("b/doc3", "");
        recorders = new ArrayList<>();
        catalog = new DynamicQueryCatalogStub("static");
        formatterRegistry = createQueryResultFormatterRegistry();
        curatorServices = new HashSet<>();
        curatorContributions = new HashSet<>();
        factories = new HashMap<>();
        factories.put("recorder", factory((configuration, _) -> {
            var recorder = new DocumentRecorder(configuration.string("folder", "none"));
            recorders.add(recorder);
            return extension(recorder);
        }).withSettings("folder"));
        factories.put("query", factory((_, _) ->
            extension(new DocumentRecorder("q")).withQueries(new QueryStub("dynamic"))));
        factories.put("formatter", factory((_, _) ->
            extension(new DocumentRecorder("f")).withFormatters(new ShoutFormatter())));
        factories.put("clash", factory((_, _) ->
            extension(new DocumentRecorder("c")).withQueries(new QueryStub("static"))));
        factories.put("orphan", factory((_, _) ->
            extension(new ChangeProcessorStub(99).consuming(Integer.class))));
        factories.put("broken", factory((_, _) -> {
            throw new IllegalStateException("Broken on purpose");
        }));
    }

    private void build()
    {
        if (orchestrator != null)
        {
            return;
        }
        registry = newExtensionRegistry(catalog, formatterRegistry, factories);
        settings = new CuratorSettings();
        var resolver = new ExtensionResolver(factories, curatorServices, curatorContributions);
        var loader =
            new ConfigurationLoader(Optional.of(CONFIGURATION_DOCUMENT), vault, resolver, registry,
                settings);
        orchestrator = new DefaultChangeProcessorOrchestrator(
            Set.of(new VaultInitializer(vault), loader),
            new NullStatistics(),
            registry
        );
    }

    private Document configure(String extensionsYaml)
    {
        return vault.addDocument(CONFIGURATION_DOCUMENT,
            "Some text.\n\n```yaml\nextensions:\n" + extensionsYaml + "\n```\n");
    }

    private Changelog startUp()
    {
        build();
        return orchestrator.runFor(List.of(RESET));
    }

    private Changelog configurationChanged(String extensionsYaml)
    {
        var document = configure(extensionsYaml);
        return orchestrator.runFor(List.of(Change.update(document, Document.class)));
    }

    private static boolean hasReset(Changelog changelog)
    {
        return changelog.changes().anyMatch(isPayloadType(Reset.class));
    }

    private void assertFailure(String extension, String reasonFragment)
    {
        softly.assertThat(registry.status().failures()).containsKey(extension);
        softly.assertThat(registry.status().failures().get(extension)).contains(reasonFragment);
        softly.assertThat(registry.status().active()).doesNotContain(extension);
    }

    // Basic loading and unloading

    @Test
    void loadExtensionsAtStartup()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        assertThat(recorders).hasSize(1);
        var recorder = recorders.getFirst();
        softly.assertThat(recorder.documents()).containsExactly("doc1", "doc2");
        softly.assertThat(orchestrator.changeProcessors()).contains(recorder);
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        softly.assertThat(registry.status().failures()).isEmpty();
        softly.assertThat(registry.status().lastError()).isEmpty();
    }

    @Test
    void loadExtensionWithoutConfiguration()
    {
        configure("  recorder:");
        startUp();
        assertThat(recorders).hasSize(1);
        softly.assertThat(recorders.getFirst().name()).isEqualTo("DocumentRecorder-none");
    }

    @Test
    void rejectListOfExtensions()
    {
        configure("  - recorder");
        startUp();
        softly.assertThat(recorders).isEmpty();
        softly.assertThat(registry.status().active()).isEmpty();
        softly.assertThat(registry.status().lastError()).hasValueSatisfying(error ->
            softly.assertThat(error).contains("must be a map"));
    }

    @Test
    void ignoreConfigurationInFrontMatter()
    {
        vault.addDocument(CONFIGURATION_DOCUMENT, """
            ---
            extensions:
              recorder:
            ---
            Some text.
            """);
        startUp();
        softly.assertThat(recorders).isEmpty();
        softly.assertThat(registry.status().active()).isEmpty();
    }

    @Test
    void readConfigurationFromFirstYamlCodeBlockOnly()
    {
        vault.addDocument(CONFIGURATION_DOCUMENT, """
            # Configuration

            ```java
            extensions:
              query:
            ```

            ## Extensions

            ```yaml
            extensions:
              recorder:
                folder: a
            ```

            ```yaml
            extensions:
              query:
            ```
            """);
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        assertThat(recorders).hasSize(1);
        softly.assertThat(recorders.getFirst().documents()).containsExactly("doc1", "doc2");
    }

    @Test
    void readConfigurationFromYmlCodeBlock()
    {
        vault.addDocument(CONFIGURATION_DOCUMENT, """
            ```yml
            extensions:
              recorder:
            ```
            """);
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("recorder");
    }

    @Test
    void changingTextOutsideCodeBlockDoesNotReload()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        var document = vault.addDocument(CONFIGURATION_DOCUMENT,
            "Other text.\n\n```yaml\nextensions:\n  recorder:\n    folder: a\n```\n");
        var changelog = orchestrator.runFor(List.of(Change.update(document, Document.class)));
        softly.assertThat(hasReset(changelog)).isFalse();
        softly.assertThat(recorders).hasSize(1);
    }

    @Test
    void noConfigurationDocumentNoExtensions()
    {
        startUp();
        softly.assertThat(recorders).isEmpty();
        softly.assertThat(registry.status().active()).isEmpty();
        softly.assertThat(registry.status().available().keySet())
            .containsExactly("broken", "clash", "formatter", "orphan", "query", "recorder");
    }

    @Test
    void newExtensionsReceiveCompleteVaultOnConfigurationChange()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        var changelog = configurationChanged("  recorder:\n    folder: b");
        softly.assertThat(hasReset(changelog)).isTrue();
        assertThat(recorders).hasSize(2);
        var oldRecorder = recorders.getFirst();
        var newRecorder = recorders.getLast();
        softly.assertThat(newRecorder.documents()).containsExactly("doc3");
        softly.assertThat(orchestrator.changeProcessors())
            .contains(newRecorder)
            .doesNotContain(oldRecorder);
    }

    @Test
    void unchangedExtensionConfigurationDoesNotReload()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        var changelog = configurationChanged("  recorder:\n    folder: a");
        softly.assertThat(hasReset(changelog)).isFalse();
        softly.assertThat(recorders).hasSize(1);
        softly.assertThat(vault.reloadCount()).isZero();
    }

    @Test
    void configurationChangeReloadsVault()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        softly.assertThat(vault.reloadCount()).isZero();
        configurationChanged("  recorder:\n    folder: b");
        softly.assertThat(vault.reloadCount()).isEqualTo(1);
    }

    @Test
    void rejectedConfigurationDoesNotReloadVault()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        configurationChanged("  42");
        softly.assertThat(vault.reloadCount()).isZero();
    }

    @Test
    void deletingConfigurationUnloadsAllExtensions()
    {
        var document = configure("  recorder:\n    folder: a");
        startUp();
        var changelog = orchestrator.runFor(List.of(Change.delete(document, Document.class)));
        softly.assertThat(hasReset(changelog)).isTrue();
        softly.assertThat(registry.status().active()).isEmpty();
        softly.assertThat(orchestrator.changeProcessors()).doesNotContain(recorders.getFirst());
    }

    @Test
    void registerAndUnregisterQueries()
    {
        configure("  query:");
        startUp();
        softly.assertThat(catalog.query("dynamic")).isInstanceOf(QueryStub.class);
        configurationChanged("  recorder:");
        softly.assertThat(catalog.queries()).isEmpty();
    }

    @Test
    void registerAndUnregisterQueryResultFormatters()
    {
        configure("  formatter:");
        startUp();
        var shout = new Shout("hello");
        softly.assertThat(formatterRegistry.supports(shout, OutputFormat.MARKDOWN)).isTrue();
        softly.assertThat(formatterRegistry.format(shout, OutputFormat.MARKDOWN))
            .isEqualTo("HELLO!");
        configurationChanged("  recorder:");
        softly.assertThat(formatterRegistry.supports(shout, OutputFormat.MARKDOWN)).isFalse();
    }

    /// Adds extensions that introduce output formats; not in the default set, to keep the other
    /// tests simple.
    private void addOutputFormatExtensions()
    {
        factories.put("csv", factory((_, _) ->
            extension(new DocumentRecorder("csv")).withFormatters(new ShoutFormatter(CSV))));
        factories.put("csv-too", factory((_, _) ->
            extension(new DocumentRecorder("csv2")).withFormatters(new ShoutFormatter(CSV))));
        factories.put("csv-clash", factory((_, _) ->
            extension(new DocumentRecorder("csv3")).withFormatters(
                new ShoutFormatter(new OutputFormat("csv", "application/csv")))));
        factories.put("markdown-clash", factory((_, _) ->
            extension(new DocumentRecorder("md")).withFormatters(
                new ShoutFormatter(new OutputFormat("markdown", "text/plain")))));
    }

    @Test
    void registerAndUnregisterOutputFormats()
    {
        addOutputFormatExtensions();
        configure("  csv:");
        startUp();
        var shout = new Shout("hello");
        softly.assertThat(formatterRegistry.outputFormats()).contains(CSV);
        softly.assertThat(formatterRegistry.outputFormat("csv")).contains(CSV);
        var formatted = formatterRegistry.formatResult(shout, CSV);
        softly.assertThat(formatted.outputFormat()).isEqualTo(CSV);
        softly.assertThat(formatted.output()).isEqualTo("csv:HELLO!");
        configurationChanged("  recorder:");
        softly.assertThat(formatterRegistry.outputFormats()).doesNotContain(CSV);
        softly.assertThat(formatterRegistry.outputFormat("csv")).isEmpty();
    }

    @Test
    void extensionsCanShareAnIdenticalOutputFormat()
    {
        addOutputFormatExtensions();
        configure("  csv:\n  csv-too:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("csv", "csv-too");
        softly.assertThat(registry.status().outputFormatProviders())
            .containsEntry("csv", new TreeSet<>(List.of("csv", "csv-too")));
    }

    @Test
    void dropExtensionWithClashingOutputFormat()
    {
        addOutputFormatExtensions();
        configure("  csv:\n  csv-clash:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("csv");
        assertFailure("csv-clash",
            "Output format(s) already in use with a different definition: csv.");
        softly.assertThat(formatterRegistry.outputFormat("csv")).contains(CSV);
    }

    @Test
    void dropExtensionThatRedefinesBuiltInOutputFormat()
    {
        addOutputFormatExtensions();
        configure("  markdown-clash:");
        startUp();
        softly.assertThat(registry.status().active()).isEmpty();
        assertFailure("markdown-clash",
            "Output format(s) already in use with a different definition: markdown.");
        softly.assertThat(formatterRegistry.outputFormat("markdown"))
            .contains(OutputFormat.MARKDOWN);
    }

    // Extensions are created in alphabetical order, providers before consumers; the names in the
    // conflict tests below are chosen to get a specific order.

    @Test
    void conflictLoserIsLoadedWhenWinnerIsDroppedForAnotherReason()
    {
        addOutputFormatExtensions();
        factories.put("a-csv-orphan", factory((_, _) ->
            extension(new ChangeProcessorStub(98).consuming(Integer.class))
                .withFormatters(new ShoutFormatter(CSV))));
        configure("  a-csv-orphan:\n  csv-clash:");
        startUp();
        assertFailure("a-csv-orphan", "Integer");
        softly.assertThat(registry.status().active()).containsExactly("csv-clash");
        softly.assertThat(registry.status().failures()).doesNotContainKey("csv-clash");
        softly.assertThat(formatterRegistry.outputFormat("csv"))
            .contains(new OutputFormat("csv", "application/csv"));
    }

    @Test
    void extensionThatLosesOneConflictClaimsNothing()
    {
        addOutputFormatExtensions();
        factories.put("d-both", factory((_, _) ->
            extension(new DocumentRecorder("d-both"))
                .withQueries(new QueryStub("shared"))
                .withFormatters(new ShoutFormatter(new OutputFormat("csv", "application/csv")))));
        factories.put("e-shared", factory((_, _) ->
            extension(new DocumentRecorder("shared")).withQueries(new QueryStub("shared"))));
        configure("  csv:\n  d-both:\n  e-shared:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("csv", "e-shared");
        assertFailure("d-both",
            "Output format(s) already in use with a different definition: csv.");
        softly.assertThat(catalog.query("shared")).isInstanceOf(QueryStub.class);
    }

    @Test
    void extensionWithMultipleConflictsReportsAll()
    {
        addOutputFormatExtensions();
        factories.put("d-both", factory((_, _) ->
            extension(new DocumentRecorder("d-both"))
                .withQueries(new QueryStub("static"))
                .withFormatters(new ShoutFormatter(new OutputFormat("csv", "application/csv")))));
        configure("  csv:\n  d-both:");
        startUp();
        assertFailure("d-both", "Query name(s) already in use: static.");
        assertFailure("d-both",
            "Output format(s) already in use with a different definition: csv.");
    }

    @Test
    void dependentsOfConflictLoserAreDropped()
    {
        addOutputFormatExtensions();
        factories.put("consumer", factory((_, _) -> extension(new DocumentRecorder("c")))
            .requiring(Greeter.class));
        factories.put("provider", factory((_, _) ->
            extension(new DocumentRecorder("p"))
                .withFormatters(new ShoutFormatter(new OutputFormat("csv", "application/csv")))
                .withService(Greeter.class, () -> "Hello")).providing(Greeter.class));
        configure("  csv:\n  consumer:\n  provider:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("csv");
        assertFailure("provider", "Output format(s) already in use");
        assertFailure("consumer", "Requires services of extension 'provider'");
    }

    @Test
    void stagedExtensionsAreOnlyActivatedOnReset()
    {
        startUp();
        registry.stage(new Generation(
            new LinkedHashMap<>(Map.of("recorder", extension(new DocumentRecorder("a")))),
            Map.of(),
            new TreeMap<>()
        ));
        var document = vault.addDocumentInPath("a/doc4", "");
        orchestrator.runFor(List.of(Change.create(document, Document.class)));
        softly.assertThat(registry.status().active()).isEmpty();
    }

    // Partial loading: extensions that don't fit are dropped, the others are loaded.

    @Test
    void dropUnknownExtension()
    {
        configure("  recorder:\n    folder: a\n  bogus:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        assertFailure("bogus", "Unknown extension");
    }

    @Test
    void dropExtensionWithUnsupportedSetting()
    {
        configure("  recorder:\n    folder: a\n    colour: blue\n  query:\n    any: thing");
        startUp();
        assertFailure("recorder", "Unsupported setting(s): colour. Supported: folder.");
        assertFailure("query", "This extension has no settings.");
    }

    @Test
    void dropExtensionThatFailsToCreate()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        var changelog = configurationChanged("  recorder:\n    folder: a\n  broken:");
        softly.assertThat(hasReset(changelog)).isTrue();
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        assertFailure("broken", "Broken on purpose");
    }

    @Test
    void dropExtensionWithUnsatisfiedConsumer()
    {
        configure("  recorder:\n    folder: a\n  orphan:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        assertFailure("orphan", "Stub-99 needs Integer");
        softly.assertThat(recorders.getFirst().documents()).containsExactly("doc1", "doc2");
    }

    @Test
    void dropExtensionWithClashingQuery()
    {
        configure("  query:\n  clash:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("query");
        assertFailure("clash", "Query name(s) already in use: static.");
        softly.assertThat(catalog.query("dynamic")).isInstanceOf(QueryStub.class);
    }

    @Test
    void rejectUnparseableConfigurationAndKeepCurrentExtensions()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        var changelog = configurationChanged("  42");
        softly.assertThat(hasReset(changelog)).isFalse();
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        softly.assertThat(registry.status().lastError()).isPresent();
    }

    @Test
    void acceptValidConfigurationAfterFailure()
    {
        configure("  bogus:");
        startUp();
        configurationChanged("  recorder:\n    folder: a");
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        softly.assertThat(registry.status().failures()).isEmpty();
    }

    // Services

    @Test
    void providerIsCreatedBeforeConsumerAndServiceIsInjected()
    {
        var greetings = new ArrayList<String>();
        factories.put("a-consumer", factory((_, context) -> {
            greetings.add(context.service(Greeter.class).greet());
            return extension(new DocumentRecorder("consumer"));
        }).requiring(Greeter.class));
        factories.put("z-provider", factory((_, _) ->
            extension(new DocumentRecorder("provider"))
                .withService(Greeter.class, () -> "Hello")).providing(Greeter.class));
        configure("  a-consumer:\n  z-provider:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("a-consumer", "z-provider");
        softly.assertThat(greetings).containsExactly("Hello");
    }

    @Test
    void curatorProvidesServices()
    {
        var greetings = new ArrayList<String>();
        curatorServices.add(service(Greeter.class, () -> "Hi"));
        factories.put("consumer", factory((_, context) -> {
            greetings.add(context.service(Greeter.class).greet());
            return extension(new DocumentRecorder("consumer"));
        }).requiring(Greeter.class));
        configure("  consumer:");
        startUp();
        softly.assertThat(greetings).containsExactly("Hi");
    }

    @Test
    void dropConsumerOfMissingServiceWithHint()
    {
        factories.put("consumer", factory((_, _) -> extension(new DocumentRecorder("c")))
            .requiring(Greeter.class));
        factories.put("provider", factory((_, _) -> extension(new DocumentRecorder("p"))
            .withService(Greeter.class, () -> "Hello")).providing(Greeter.class));
        configure("  consumer:");
        startUp();
        assertFailure("consumer", "Missing required service(s): Greeter (enable 'provider').");
    }

    @Test
    void dropConsumerOfServiceWithoutProvider()
    {
        factories.put("consumer", factory((_, _) -> extension(new DocumentRecorder("c")))
            .requiring(Greeter.class));
        configure("  consumer:");
        startUp();
        assertFailure("consumer", "Greeter (no extension provides it)");
    }

    @Test
    void dropConsumerWhenProviderFailsToCreate()
    {
        factories.put("consumer", factory((_, _) -> extension(new DocumentRecorder("c")))
            .requiring(Greeter.class));
        factories.put("provider", factory((_, _) -> {
            throw new IllegalStateException("Broken on purpose");
        }).providing(Greeter.class));
        configure("  consumer:\n  provider:");
        startUp();
        assertFailure("provider", "Broken on purpose");
        assertFailure("consumer", "provided by 'provider', which failed to load");
    }

    @Test
    void dropConsumerWhenProviderIsDroppedAtActivation()
    {
        factories.put("consumer", factory((_, _) -> extension(new DocumentRecorder("c")))
            .requiring(Greeter.class));
        factories.put("provider", factory((_, _) ->
            extension(new ChangeProcessorStub(99).consuming(Integer.class))
                .withService(Greeter.class, () -> "Hello")).providing(Greeter.class));
        configure("  consumer:\n  provider:");
        startUp();
        assertFailure("provider", "Integer");
        assertFailure("consumer", "Requires services of extension 'provider'");
    }

    @Test
    void dropAllProvidersOfTheSameService()
    {
        factories.put("one", factory((_, _) -> extension(new DocumentRecorder("1"))
            .withService(Greeter.class, () -> "1")).providing(Greeter.class));
        factories.put("two", factory((_, _) -> extension(new DocumentRecorder("2"))
            .withService(Greeter.class, () -> "2")).providing(Greeter.class));
        configure("  one:\n  two:\n  recorder:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        assertFailure("one", "provided by more than one extension: one, two");
        assertFailure("two", "provided by more than one extension: one, two");
    }

    @Test
    void dropExtensionProvidingCuratorService()
    {
        curatorServices.add(service(Greeter.class, () -> "Hi"));
        factories.put("provider", factory((_, _) -> extension(new DocumentRecorder("p"))
            .withService(Greeter.class, () -> "Hello")).providing(Greeter.class));
        configure("  provider:");
        startUp();
        assertFailure("provider", "which the curator already provides");
    }

    @Test
    void dropExtensionsInServiceDependencyCycle()
    {
        factories.put("one", factory((_, _) -> extension(new DocumentRecorder("1"))
            .withService(Greeter.class, () -> "1"))
            .providing(Greeter.class).requiring(Topping.class));
        factories.put("two", factory((_, _) -> extension(new DocumentRecorder("2"))
            .withService(Topping.class, new Topping("2")))
            .providing(Topping.class).requiring(Greeter.class));
        configure("  one:\n  two:\n  recorder:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        assertFailure("one", "Circular service dependency between extensions: one, two.");
        assertFailure("two", "Circular service dependency between extensions: one, two.");
    }

    @Test
    void dropExtensionThatRegistersUndeclaredService()
    {
        factories.put("liar", factory((_, _) -> extension(new DocumentRecorder("l"))
            .withService(Greeter.class, () -> "Hello")));
        configure("  liar:");
        startUp();
        assertFailure("liar",
            "Registered services (Greeter) don't match the declared services (none).");
    }

    @Test
    void dropExtensionThatGetsUndeclaredService()
    {
        curatorServices.add(service(Greeter.class, () -> "Hi"));
        factories.put("sneaky", factory((_, context) -> {
            context.service(Greeter.class);
            return extension(new DocumentRecorder("s"));
        }));
        configure("  sneaky:");
        startUp();
        assertFailure("sneaky", "doesn't declare that it requires service Greeter");
    }

    // Contributions (whiteboard)

    @Test
    void ownerOfExtensionPointCollectsContributionsFromItsConsumers()
    {
        var toppings = new ArrayList<Topping>();
        // The pizza owns the extension point and provides a service...
        factories.put("pizza", factory((_, context) -> {
            toppings.addAll(context.contributionsTo(TOPPINGS));
            return extension(new DocumentRecorder("pizza"))
                .withService(Greeter.class, () -> "Pizza!");
        }).providing(Greeter.class));
        // ...that the cheese requires, while contributing to the extension point.
        factories.put("cheese", factory((_, context) -> {
            context.service(Greeter.class);
            return extension(new DocumentRecorder("cheese"));
        }).requiring(Greeter.class).contributing(TOPPINGS, new Topping("cheese")));
        curatorContributions.add(TOPPINGS.contribute(new Topping("tomato")));
        configure("  pizza:\n  cheese:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("cheese", "pizza");
        softly.assertThat(toppings)
            .containsExactlyInAnyOrder(new Topping("tomato"), new Topping("cheese"));
    }

    @Test
    void contributionsOfDroppedExtensionsAreNotCollected()
    {
        var toppings = new ArrayList<Topping>();
        factories.put("pizza", factory((_, context) -> {
            toppings.clear();
            toppings.addAll(context.contributionsTo(TOPPINGS));
            return extension(new DocumentRecorder("pizza"));
        }));
        factories.put("anchovies", factory((_, _) -> {
            throw new IllegalStateException("Nobody likes anchovies");
        }).contributing(TOPPINGS, new Topping("anchovies")));
        configure("  pizza:\n  anchovies:");
        startUp();
        softly.assertThat(registry.status().active()).containsExactly("pizza");
        softly.assertThat(toppings).isEmpty();
    }

    // Lifecycle

    @Test
    void startExtensionsOnActivationAndStopThemWhenReplaced()
    {
        var lifecycles = new ArrayList<LifecycleRecorder>();
        factories.put("lifecycle", factory((_, _) -> {
            var lifecycle = new LifecycleRecorder();
            lifecycles.add(lifecycle);
            return extension(new DocumentRecorder("l")).withLifecycle(lifecycle);
        }).withSettings("version"));
        configure("  lifecycle:\n    version: 1");
        startUp();
        assertThat(lifecycles).hasSize(1);
        softly.assertThat(lifecycles.getFirst().starts).isEqualTo(1);
        softly.assertThat(lifecycles.getFirst().stops).isZero();
        configurationChanged("  lifecycle:\n    version: 2");
        assertThat(lifecycles).hasSize(2);
        softly.assertThat(lifecycles.getFirst().stops).isEqualTo(1);
        softly.assertThat(lifecycles.getLast().starts).isEqualTo(1);
        softly.assertThat(lifecycles.getLast().stops).isZero();
    }

    @Test
    void droppedExtensionsAreNeverStarted()
    {
        var lifecycle = new LifecycleRecorder();
        factories.put("lifecycle", factory((_, _) ->
            extension(new ChangeProcessorStub(99).consuming(Integer.class))
                .withLifecycle(lifecycle)));
        configure("  lifecycle:");
        startUp();
        assertFailure("lifecycle", "Integer");
        softly.assertThat(lifecycle.starts).isZero();
    }

    // Reporting

    @Test
    void configurationQueryReportsExtensionStatus()
    {
        factories.put("consumer", factory((_, _) -> extension(new DocumentRecorder("c")))
            .requiring(Greeter.class));
        configure("  recorder:\n    folder: a\n  bogus:\n  consumer:");
        startUp();
        var output = runConfigurationQuery();
        softly.assertThat(output).contains("- 🟢 **recorder**: Stub");
        softly.assertThat(output).contains("    - settings:\n        - `folder`: Setting folder\n");
        softly.assertThat(output).contains("- ⚪️️**query**: Stub");
        softly.assertThat(output).contains("- 🔴 **bogus**");
        softly.assertThat(output).contains("    - not loaded: Unknown extension.");
        softly.assertThat(output).contains("- 🔴 **consumer**: Stub");
        softly.assertThat(output).contains("    - requires: Greeter");
    }

    @Test
    void configurationQueryReportsDefaultSettings()
    {
        configure("  recorder:");
        startUp();
        var output = runConfigurationQuery();
        softly.assertThat(output).contains("- **delay**: 3 seconds (default)");
        softly.assertThat(output).contains("- **locale**: en-US (English (United States)) (default)");
    }

    @Test
    void configurationQueryReportsConfiguredSettings()
    {
        vault.addDocument(CONFIGURATION_DOCUMENT,
            "```yaml\ndelay: 1\nlocale: nl-NL\nextensions:\n  recorder:\n```\n");
        startUp();
        var output = runConfigurationQuery();
        softly.assertThat(output).contains("- **delay**: 1 second\n");
        softly.assertThat(output).contains("- **locale**: nl-NL (Dutch (Netherlands))\n");
        softly.assertThat(output).contains("- 🟢 **recorder**: Stub");
    }

    @Test
    void configurationQueryReportsAvailableQueries()
    {
        configure("  query:");
        startUp();
        var output = runConfigurationQuery();
        softly.assertThat(output).contains("### Queries\n\n- **dynamic**: Stub\n");
    }

    @Test
    void configurationQueryReportsNoQueries()
    {
        configure("  recorder:");
        startUp();
        var output = runConfigurationQuery();
        softly.assertThat(output).contains("### Queries\n\nNo queries are available.\n");
    }

    @Test
    void configurationQueryReportsOutputFormats()
    {
        addOutputFormatExtensions();
        configure("  csv:");
        startUp();
        var output = runConfigurationQuery();
        softly.assertThat(output).contains("### Output formats\n\n" +
                                           "- **csv**: `text/csv`, fenced, from csv\n" +
                                           "- **json**: `application/json`, fenced\n" +
                                           "- **markdown**: `text/markdown`, inline\n");
    }

    private String runConfigurationQuery()
    {
        var query = new ConfigurationQuery(
            settings, registry, () -> catalog, formatterRegistry, Optional.of(CONFIGURATION_DOCUMENT));
        return formatterRegistry
            .format(query.run(emptyQueryBlock()), OutputFormat.MARKDOWN);
    }

    record Shout(String text)
        implements QueryResult
    {
    }

    static final OutputFormat CSV = new OutputFormat("csv", "text/csv");

    static final class ShoutFormatter
        implements QueryResultFormatter<Shout>
    {
        private final OutputFormat outputFormat;

        ShoutFormatter()
        {
            this(OutputFormat.MARKDOWN);
        }

        ShoutFormatter(OutputFormat outputFormat)
        {
            this.outputFormat = outputFormat;
        }

        @Override
        public Class<Shout> queryResultType()
        {
            return Shout.class;
        }

        @Override
        public OutputFormat outputFormat()
        {
            return outputFormat;
        }

        @Override
        public String format(Shout result)
        {
            var shout = result.text().toUpperCase(Locale.ROOT) + "!";
            return outputFormat.equals(OutputFormat.MARKDOWN)
                   ? shout
                   : outputFormat.name() + ":" + shout;
        }
    }

    record QueryStub(String name)
        implements Query
    {
        @Override
        public String description()
        {
            return "Stub";
        }

        @Override
        public Map<String, String> supportedConfiguration()
        {
            return Map.of();
        }

        @Override
        public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
        {
            return false;
        }

        @Override
        public QueryResult run(QueryDefinition definition)
        {
            throw new UnsupportedOperationException("Not needed for this test");
        }
    }
}

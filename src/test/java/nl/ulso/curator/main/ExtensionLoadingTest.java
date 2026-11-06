package nl.ulso.curator.main;

import nl.ulso.curator.change.*;
import nl.ulso.curator.extension.ExtensionFactory;
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
import static nl.ulso.curator.main.ExtensionStub.extension;
import static nl.ulso.curator.main.ExtensionTestSupport.newExtensionRegistry;
import static nl.ulso.curator.query.QueryTestModule.createQueryResultFactory;
import static nl.ulso.curator.query.QueryTestModule.createQueryResultFormatterRegistry;
import static nl.ulso.curator.vault.QueryBlockTest.emptyQueryBlock;
import static org.assertj.core.api.Assertions.assertThat;

/// Tests the complete flow of loading and unloading extensions: the [ExtensionLoader] reads the
/// configuration document, the [DefaultChangeProcessorOrchestrator] activates the extensions.
@ExtendWith(SoftAssertionsExtension.class)
class ExtensionLoadingTest
{
    private static final String CONFIGURATION_DOCUMENT = "config";

    @InjectSoftAssertions
    private SoftAssertions softly;

    private VaultStub vault;
    private List<DocumentRecorder> recorders;
    private DynamicQueryCatalogStub catalog;
    private ExtensionRegistry registry;
    private DefaultChangeProcessorOrchestrator orchestrator;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        vault.addDocumentInPath("a/doc1", "");
        vault.addDocumentInPath("a/doc2", "");
        vault.addDocumentInPath("b/doc3", "");
        recorders = new ArrayList<>();
        catalog = new DynamicQueryCatalogStub("static");
        var factories = Map.<String, ExtensionFactory>of(
            "recorder", configuration -> {
                var recorder = new DocumentRecorder(configuration.string("folder", "none"));
                recorders.add(recorder);
                return extension(recorder);
            },
            "query", _ -> extension(new DocumentRecorder("q"))
                .withQueries(new QueryStub("dynamic")),
            "clash", _ -> extension(new DocumentRecorder("c"))
                .withQueries(new QueryStub("static")),
            "orphan", _ -> extension(new ChangeProcessorStub(99).consuming(Integer.class)),
            "broken", _ -> {
                throw new IllegalStateException("Broken on purpose");
            }
        );
        registry = newExtensionRegistry(catalog, factories);
        var loader =
            new ExtensionLoader(Optional.of(CONFIGURATION_DOCUMENT), vault, factories, registry);
        orchestrator = new DefaultChangeProcessorOrchestrator(
            Set.of(new VaultInitializer(vault), loader),
            new NullStatistics(),
            registry
        );
    }

    private Document configure(String extensionsYaml)
    {
        return vault.addDocument(CONFIGURATION_DOCUMENT,
            "---\nextensions:\n" + extensionsYaml + "\n---\n\nSome text.\n");
    }

    private Changelog startUp()
    {
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
        softly.assertThat(registry.status().lastError()).isEmpty();
    }

    @Test
    void loadExtensionsFromList()
    {
        configure("  - recorder");
        startUp();
        assertThat(recorders).hasSize(1);
        softly.assertThat(recorders.getFirst().name()).isEqualTo("DocumentRecorder-none");
    }

    @Test
    void noConfigurationDocumentNoExtensions()
    {
        startUp();
        softly.assertThat(recorders).isEmpty();
        softly.assertThat(registry.status().active()).isEmpty();
        softly.assertThat(registry.status().available())
            .containsExactly("broken", "clash", "orphan", "query", "recorder");
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
    void rejectUnknownExtension()
    {
        configure("  bogus:");
        startUp();
        softly.assertThat(registry.status().active()).isEmpty();
        softly.assertThat(registry.status().lastError()).hasValueSatisfying(error ->
            assertThat(error).contains("Unknown extension(s): bogus"));
    }

    @Test
    void rejectExtensionThatFailsToCreate()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        var changelog = configurationChanged("  broken:");
        softly.assertThat(hasReset(changelog)).isFalse();
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        softly.assertThat(registry.status().lastError()).hasValueSatisfying(error ->
            assertThat(error).contains("Broken on purpose"));
    }

    @Test
    void rejectExtensionWithUnsatisfiedConsumerAndKeepCurrentExtensions()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        var recorder = recorders.getFirst();
        configurationChanged("  recorder:\n    folder: b\n  orphan:");
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        softly.assertThat(registry.status().lastError()).hasValueSatisfying(error ->
            assertThat(error).contains("unsatisfied consumer"));
        softly.assertThat(orchestrator.changeProcessors()).contains(recorder);
        // The current extensions were reset, and received the complete vault again.
        softly.assertThat(recorder.resetCount()).isPositive();
        softly.assertThat(recorder.documents()).containsExactly("doc1", "doc2");
    }

    @Test
    void rejectExtensionWithClashingQueryAndKeepCurrentExtensions()
    {
        configure("  query:");
        startUp();
        configurationChanged("  clash:");
        softly.assertThat(registry.status().active()).containsExactly("query");
        softly.assertThat(registry.status().lastError()).isPresent();
        softly.assertThat(catalog.query("dynamic")).isInstanceOf(QueryStub.class);
    }

    @Test
    void acceptValidConfigurationAfterRejection()
    {
        configure("  bogus:");
        startUp();
        configurationChanged("  recorder:\n    folder: a");
        softly.assertThat(registry.status().active()).containsExactly("recorder");
        softly.assertThat(registry.status().lastError()).isEmpty();
    }

    @Test
    void stagedExtensionsAreOnlyActivatedOnReset()
    {
        startUp();
        registry.stage(Map.of("recorder", extension(new DocumentRecorder("a"))));
        var document = vault.addDocumentInPath("a/doc4", "");
        orchestrator.runFor(List.of(Change.create(document, Document.class)));
        softly.assertThat(registry.status().active()).isEmpty();
        orchestrator.runFor(List.of(RESET));
        softly.assertThat(registry.status().active()).isEmpty();
    }

    @Test
    void extensionsQueryReportsStatus()
    {
        configure("  recorder:\n    folder: a");
        startUp();
        configurationChanged("  bogus:");
        var query = new ExtensionsQuery(
            registry, createQueryResultFactory(), Optional.of(CONFIGURATION_DOCUMENT));
        var output = createQueryResultFormatterRegistry()
            .format(query.run(emptyQueryBlock()), OutputFormat.MARKDOWN);
        softly.assertThat(output).contains("- ✅ recorder");
        softly.assertThat(output).contains("- ⬜️ query");
        softly.assertThat(output).contains("rejected**: Unknown extension(s): bogus");
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

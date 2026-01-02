package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.FrontMatterUpdateCollector;
import nl.ulso.markdown_curator.project.ProjectTestData.AttributeRegistryStub;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.Dictionary;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class FrontMatterPropertyWriterTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    private VaultStub vault;
    private AttributeRegistryStub registry;
    private FrontMatterPropertyWriter writer;
    private FrontMatterUpdateCollectorStub collector;

    @BeforeEach
    void setUp()
    {
        vault = ProjectTestData.createTestVault();
        registry = ProjectTestData.createAttributeRegistry(vault);
        collector = new FrontMatterUpdateCollectorStub();
        writer = new FrontMatterPropertyWriter(registry, collector);
    }

    @Test
    void consumedObjectTypes()
    {
        assertThat(writer.consumedObjectTypes())
            .containsAll(Set.of(AttributeRegistryUpdate.class));
    }

    @Test
    void producedObjectTypes()
    {
        assertThat(writer.producedObjectTypes()).isEmpty();
    }

    @Test
    void attributeValuesBecomeFrontMatter()
    {
        registry
            .withAttribute(
                vault.resolveDocumentInPath("Projects/Project 1"),
                "status",
                "NEW FRONTMATTER"
            )
            .withAttribute(
                vault.resolveDocumentInPath("Projects/Project 2"),
                "last_modified",
                LocalDate.of(2026, 1, 2)
            );
        writer.run(changelogFor(AttributeRegistryUpdate.REGISTRY_CHANGE));
        softly.assertThat(
                collector.frontMatterUpdates.get("Project 1").string("status", null))
            .isEqualTo("NEW FRONTMATTER");
        softly.assertThat(
                collector.frontMatterUpdates.get("Project 2").date("last_modified", null))
            .isEqualTo(LocalDate.of(2026, 1, 2));
    }

    private static class FrontMatterUpdateCollectorStub
        implements FrontMatterUpdateCollector
    {
        private final Map<String, MutableDictionary> frontMatterUpdates = new HashMap<>();

        @Override
        public Optional<Dictionary> frontMatterFor(Document document)
        {
            return Optional.empty();
        }

        @Override
        public void updateFrontMatterFor(
            Document document,
            Consumer<MutableDictionary> dictionaryConsumer)
        {
            var dictionary = frontMatterUpdates
                .computeIfAbsent(document.name(), _ -> Dictionary.mutableDictionary());
            dictionaryConsumer.accept(dictionary);
        }
    }
}

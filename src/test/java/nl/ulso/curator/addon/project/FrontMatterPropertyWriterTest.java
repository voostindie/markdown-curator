package nl.ulso.curator.addon.project;

import nl.ulso.curator.FrontMatterCollector;
import nl.ulso.dictionary.MutableDictionary;
import nl.ulso.curator.addon.project.ProjectTestData.AttributeRegistryStub;
import nl.ulso.curator.vault.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;

import static nl.ulso.curator.changelog.Change.create;
import static nl.ulso.curator.changelog.Change.delete;
import static nl.ulso.curator.changelog.Changelog.changelogFor;
import static nl.ulso.curator.addon.project.ProjectTestData.ATTRIBUTE_DEFINITIONS;
import static nl.ulso.dictionary.Dictionary.mutableDictionary;
import static org.assertj.core.api.Assertions.assertThat;

class FrontMatterPropertyWriterTest
{
    private VaultStub vault;
    private AttributeRegistryStub registry;
    private FrontMatterPropertyWriter writer;
    private FrontMatterCollectorStub collector;

    @BeforeEach
    void setUp()
    {
        vault = ProjectTestData.createTestVault();
        registry = ProjectTestData.createAttributeRegistry(vault);
        collector = new FrontMatterCollectorStub();
        writer = new FrontMatterPropertyWriter(registry, collector);
    }

    @Test
    void consumedPayloadTypes()
    {
        assertThat(writer.consumedPayloadTypes())
            .containsAll(Set.of(AttributeRegistryUpdate.class));
    }

    @Test
    void producedPayloadTypes()
    {
        assertThat(writer.producedPayloadTypes()).isEmpty();
    }

    @Test
    void attributeValueUpdateBecomesFrontMatter()
    {
        registry.withAttribute(
            vault.resolveDocumentInPath("Projects/Project 1"),
            "status",
            "NEW FRONTMATTER"
        );
        var changelog = changelogFor(
            create(
                new AttributeValue(
                    new Project(vault.resolveDocumentInPath("Projects/Project 1")),
                    ATTRIBUTE_DEFINITIONS.get("status"),
                    "NEW FRONTMATTER",
                    0
                ),
                AttributeValue.class
            ),
            AttributeRegistryUpdate.REGISTRY_CHANGE.iterator().next()
        );
        writer.run(changelog);
        assertThat(collector.frontMatterUpdates.get("Project 1").string("status", null))
            .isEqualTo("NEW FRONTMATTER");
    }

    @Test
    void attributeValueDeleteRemovesFrontMatter()
    {
        var changelog = changelogFor(
            delete(
                new AttributeValue(
                    new Project(vault.resolveDocumentInPath("Projects/Project 3")),
                    ATTRIBUTE_DEFINITIONS.get("status"),
                    null,
                    0
                ),
                AttributeValue.class
            ),
            AttributeRegistryUpdate.REGISTRY_CHANGE.iterator().next()
        );
        var dictionary = mutableDictionary();
        dictionary.setProperty("status", "NEW FRUNTMATTER");
        collector.frontMatterUpdates.put("Project 3", dictionary);
        writer.run(changelog);
        assertThat(collector.frontMatterUpdates.get("Project 3").string("status", null))
            .isNull();
    }

    private static class FrontMatterCollectorStub
        implements FrontMatterCollector
    {
        private final Map<String, MutableDictionary> frontMatterUpdates = new HashMap<>();

        @Override
        public void updateFrontMatterFor(
            Document document,
            Consumer<MutableDictionary> dictionaryConsumer)
        {
            var dictionary = frontMatterUpdates
                .computeIfAbsent(document.name(), _ -> mutableDictionary());
            dictionaryConsumer.accept(dictionary);
        }
    }
}

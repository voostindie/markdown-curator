package nl.ulso.curator.addon.project;

import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static nl.ulso.curator.changelog.Change.delete;
import static nl.ulso.curator.changelog.Change.update;
import static nl.ulso.curator.changelog.Changelog.changelogFor;
import static nl.ulso.curator.addon.project.ProjectTestData.ATTRIBUTE_DEFINITIONS;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class AttributeRegistryImplTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    private VaultStub vault;
    private AttributeRegistryImpl registry;

    @BeforeEach
    void setUp()
    {
        vault = ProjectTestData.createTestVault();
        registry = new AttributeRegistryImpl(ATTRIBUTE_DEFINITIONS);
    }

    @Test
    void newRegistry()
    {
        assertThat(registry.attributeDefinitions()).hasSameElementsAs(
            ATTRIBUTE_DEFINITIONS.values());
    }

    @Test
    void consumedPayloadTypes()
    {
        assertThat(registry.consumedPayloadTypes())
            .containsAll(Set.of(Project.class, AttributeValue.class));
    }

    @Test
    void producedPayloadTypes()
    {
        assertThat(registry.producedPayloadTypes()).containsAll(
            Set.of(AttributeRegistryUpdate.class));
    }

    @Test
    void consumesAttributeValues()
    {
        var project = new Project(vault.resolveDocumentInPath("Projects/Project 1"));
        var changelog = registry.run(changelogFor(update(new AttributeValue(
                project, ATTRIBUTE_DEFINITIONS.get("status"), "In progress", 0),
            AttributeValue.class
        )));
        softly.assertThat(changelog.isEmpty()).isFalse();
        softly.assertThat(registry.attributeValue(project, "status"))
            .map(o -> (String) o)
            .hasValue("In progress");
    }

    @Test
    void handlesAttributeValueDeletions()
    {
        var project = new Project(vault.resolveDocumentInPath("Projects/Project 1"));
        var status = ATTRIBUTE_DEFINITIONS.get("status");
        var change1 = update(
            new AttributeValue(project, status, "High", 100),
            AttributeValue.class
        );
        var change2 = update(
            new AttributeValue(project, status, "Low", 0),
            AttributeValue.class
        );
        var change3 = delete(
            new AttributeValue(project, status, null, 100),
            AttributeValue.class
        );
        var changelog = registry.run(changelogFor(change1, change2, change3));
        softly.assertThat(changelog.isEmpty()).isFalse();
        softly.assertThat(registry.attributeValue(project, status))
            .map(o -> (String) o)
            .hasValue("Low");
    }

    @Test
    void handlesProjectDeletions()
    {
        var project = new Project(vault.resolveDocumentInPath("Projects/Project 1"));
        var status = ATTRIBUTE_DEFINITIONS.get("status");
        var change1 = update(
            new AttributeValue(project, status, "High", 100),
            AttributeValue.class
        );
        var change2 = delete(
            project,
            Project.class
        );
        var changelog = registry.run(changelogFor(change1, change2));
        softly.assertThat(changelog.isEmpty()).isFalse();
        softly.assertThat(registry.attributeValue(project, status)).isEmpty();
        softly.assertThat(registry.projects()).isEmpty();
    }
}

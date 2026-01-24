package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.VaultStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nl.ulso.markdown_curator.Change.Kind.CREATE;
import static nl.ulso.markdown_curator.Change.Kind.DELETE;
import static nl.ulso.markdown_curator.Change.delete;
import static nl.ulso.markdown_curator.Change.update;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static nl.ulso.markdown_curator.project.ProjectTestData.ATTRIBUTE_DEFINITIONS;
import static org.assertj.core.api.Assertions.assertThat;

class FrontMatterAttributeProducerTest
{
    private VaultStub vault;
    private FrontMatterAttributeProducer producer;

    @BeforeEach
    void setUp()
    {
        vault = ProjectTestData.createTestVault();
        producer = new FrontMatterAttributeProducer(ATTRIBUTE_DEFINITIONS, vault);
    }

    @Test
    void consumedPayloadTypes()
    {
        assertThat(producer.consumedPayloadTypes())
            .containsAll(Set.of(Project.class));
    }

    @Test
    void producedPayloadTypes()
    {
        assertThat(producer.producedPayloadTypes())
            .containsAll(Set.of(AttributeValue.class));
    }

    @Test
    void projectDeletionRemovesAllAttributes()
    {
        var project = new Project(vault.resolveDocumentInPath("Projects/Project 1"));
        var changelog = producer.run(changelogFor(delete(project, Project.class)));
        var changes = changelog.changesFor(AttributeValue.class)
            .filter(change -> change.kind() == DELETE)
            .map(change -> change.value().definition().frontMatterProperty())
            .toList();
        assertThat(changes).contains("last_modified", "lead", "priority", "status");
    }

    @Test
    void projectUpdateAddsAttributes()
    {
        var project = new Project(vault.resolveDocumentInPath("Projects/Project 1"));
        var changelog = producer.run(changelogFor(update(project, Project.class)));
        var changes = changelog.changesFor(AttributeValue.class)
            .filter(change -> change.kind() == CREATE)
            .map(change -> change.value().definition().frontMatterProperty())
            .toList();
        assertThat(changes).contains("last_modified", "lead", "status");
    }
}

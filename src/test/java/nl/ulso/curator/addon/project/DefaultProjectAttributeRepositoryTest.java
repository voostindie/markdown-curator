package nl.ulso.curator.addon.project;

import nl.ulso.curator.statistics.MeasurementCollectorStub;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.curator.addon.project.ProjectTestData.ATTRIBUTE_DEFINITIONS;
import static nl.ulso.curator.addon.project.ProjectTestData.createTestVault;
import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class DefaultProjectAttributeRepositoryTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    private VaultStub vault;
    private DefaultProjectAttributeRepository repository;

    @BeforeEach
    void setUp()
    {
        vault = createTestVault();
        repository = new DefaultProjectAttributeRepository(ATTRIBUTE_DEFINITIONS);
    }

    @Test
    void newRepository()
    {
        assertThat(repository.attributeDefinitions()).hasSameElementsAs(
            ATTRIBUTE_DEFINITIONS.values());
    }

    @Test
    void consumedPayloadTypes()
    {
        assertThat(repository.consumedPayloadTypes())
            .containsExactlyInAnyOrder(Project.class, ProjectAttributeValue.class);
    }

    @Test
    void producedPayloadTypes()
    {
        assertThat(repository.producedPayloadTypes()).containsExactly(
            ProjectAttributeRepositoryUpdate.class);
    }

    @Test
    void consumesAttributeValues()
    {
        var project = new Project(vault.resolveDocumentInPath("Projects/Project 1"));
        var changelog = repository.apply(changelogFor(
            create(project, Project.class),
            update(
                new ProjectAttributeValue(project, ATTRIBUTE_DEFINITIONS.get("status"),
                    "In progress", 0
                ),
                ProjectAttributeValue.class
            )
        ));
        softly.assertThat(changelog.isEmpty()).isFalse();
        softly.assertThat(repository.valueOf(project, "status"))
            .map(o -> (String) o)
            .hasValue("In progress");
        var measurements = new MeasurementCollectorStub();
        repository.collectMeasurements(measurements);
        softly.assertThat(measurements.totalFor("project", "weighted_value")).isEqualTo(1);
        softly.assertThat(measurements.totalFor("project", "project_attribute_value")).isEqualTo(1);
    }

    @Test
    void handlesAttributeValueDeletions()
    {
        var project = new Project(vault.resolveDocumentInPath("Projects/Project 1"));
        var status = ATTRIBUTE_DEFINITIONS.get("status");
        var init = create(project, Project.class);
        var change1 = update(
            new ProjectAttributeValue(project, status, "High", 100),
            ProjectAttributeValue.class
        );
        var change2 = update(
            new ProjectAttributeValue(project, status, "Low", 0),
            ProjectAttributeValue.class
        );
        var change3 = delete(
            new ProjectAttributeValue(project, status, null, 100),
            ProjectAttributeValue.class
        );
        var changelog = repository.apply(changelogFor(init, change1, change2, change3));
        softly.assertThat(changelog.isEmpty()).isFalse();
        softly.assertThat(repository.valueOf(project, status))
            .map(o -> (String) o)
            .hasValue("Low");
    }

    @Test
    void handlesProjectDeletions()
    {
        var project = new Project(vault.resolveDocumentInPath("Projects/Project 1"));
        var status = ATTRIBUTE_DEFINITIONS.get("status");
        var init = create(project, Project.class);
        var change1 = update(
            new ProjectAttributeValue(project, status, "High", 100),
            ProjectAttributeValue.class
        );
        var change2 = delete(project, Project.class);
        var changelog = repository.apply(changelogFor(init, change1, change2));
        softly.assertThat(changelog.isEmpty()).isFalse();
        softly.assertThat(repository.valueOf(project, status)).isNotPresent();
    }
}
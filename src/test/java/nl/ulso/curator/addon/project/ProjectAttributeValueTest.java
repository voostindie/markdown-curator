package nl.ulso.curator.addon.project;

import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.newAttributeDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SoftAssertionsExtension.class)
class ProjectAttributeValueTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    private VaultStub vault;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        vault.addDocumentInPath("Project 1", "");
        vault.addDocumentInPath("Project 2", "");
    }

    @AfterEach
    void tearDown()
    {
        vault = null;
    }

    @Test
    void newAttributeValue()
    {
        var project = new Project(vault.resolveDocumentInPath("Project 1"));
        var definition = newAttributeDefinition(String.class, "status");
        var value = new ProjectAttributeValue(project, definition, "draft", 0);
        assertThat(value.value()).isEqualTo("draft");
    }

    @Test
    void newAttributeValueWithTypeDifferentFromDefinitionThrows()
    {
        var project = new Project(vault.resolveDocumentInPath("Project 1"));
        var definition = newAttributeDefinition(String.class, "status");
        assertThatThrownBy(() -> new ProjectAttributeValue(project, definition, 42, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toWeightedValue()
    {
        var project = new Project(vault.resolveDocumentInPath("Project 1"));
        var definition = newAttributeDefinition(Integer.class, "priority");
        var value = new ProjectAttributeValue(project, definition, 42, 100);
        var weightedValue = value.toWeightedValue();
        softly.assertThat(weightedValue.value()).isEqualTo(42);
        softly.assertThat(weightedValue.weight()).isEqualTo(100);
        softly.assertThat(weightedValue).isEqualTo(new WeightedValue(42, 100));
    }

    @Test
    void equalsOnlyForSameProject()
    {
        var project1 = new Project(vault.resolveDocumentInPath("Project 1"));
        var priority = newAttributeDefinition(Integer.class, "priority");
        var value1 = new ProjectAttributeValue(project1, priority, 42, 100);
        var project2 = new Project(vault.resolveDocumentInPath("Project 2"));
        var value2 = new ProjectAttributeValue(project2, priority, 42, 100);
        assertThatThrownBy(
            () -> value1.equals(value2)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsOnlyForSameDefinition()
    {
        var project = new Project(vault.resolveDocumentInPath("Project 1"));
        var priority = newAttributeDefinition(Integer.class, "priority");
        var status = newAttributeDefinition(String.class, "status");
        var value1 = new ProjectAttributeValue(project, priority, 42, 100);
        var value2 = new ProjectAttributeValue(project, status, "In progress", 100);
        assertThatThrownBy(
            () -> value1.equals(value2)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stringRepresentation()
    {
        var project = new Project(vault.resolveDocumentInPath("Project 1"));
        var priority = newAttributeDefinition(Integer.class, "priority");
        var value = new ProjectAttributeValue(project, priority, 42, 100);
        assertThat(value).hasToString("Project 1', priority: '42', weight: '100");
    }

}

package nl.ulso.curator.addon.project;

import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.curator.addon.project.AttributeDefinition.newAttributeDefinition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SoftAssertionsExtension.class)
class AttributeValueTest
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
        var value = new AttributeValue(project, definition, "draft", 0);
        assertThat(value.value()).isEqualTo("draft");
    }

    @Test
    void newAttributeValueWithTypeDifferentFromDefinitionThrows()
    {
        var project = new Project(vault.resolveDocumentInPath("Project 1"));
        var definition = newAttributeDefinition(String.class, "status");
        assertThatThrownBy(() -> new AttributeValue(project, definition, 42, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void comparingAttributeValuesForDifferentProjectsThrow()
    {
        var project1 = new Project(vault.resolveDocumentInPath("Project 1"));
        var project2 = new Project(vault.resolveDocumentInPath("Project 2"));
        var definition = newAttributeDefinition(String.class, "status");
        var value1 = new AttributeValue(project1, definition, "draft", 0);
        var value2 = new AttributeValue(project2, definition, "draft", 0);
        softly.assertThatThrownBy(() -> value1.compareTo(value2))
            .isInstanceOf(IllegalArgumentException.class);
        softly.assertThatThrownBy(() -> value1.equals(value2))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void comparingAttributeValuesForDifferentDefinitionsThrow()
    {
        var project = new Project(vault.resolveDocumentInPath("Project 1"));
        var definition1 = newAttributeDefinition(String.class, "status");
        var definition2 = newAttributeDefinition(Integer.class, "priority");
        var value1 = new AttributeValue(project, definition1, "draft", 0);
        var value2 = new AttributeValue(project, definition2, 42, 0);
        softly.assertThatThrownBy(() -> value1.compareTo(value2))
            .isInstanceOf(IllegalArgumentException.class);
        softly.assertThatThrownBy(() -> value1.equals(value2))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void comparingAttributeValuesWithDifferentWeightsAreNotEqual()
    {
        var project = new Project(vault.resolveDocumentInPath("Project 1"));
        var definition = newAttributeDefinition(String.class, "status");
        var value1 = new AttributeValue(project, definition, "draft", 0);
        var value2 = new AttributeValue(project, definition, "draft", 1);
        softly.assertThat(value1.compareTo(value2)).isNotEqualTo(0);
        softly.assertThat(value1.equals(value2)).isFalse();
    }

    @Test
    void comparingAttributeValuesWithSameWeightsAreEqual()
    {
        var project = new Project(vault.resolveDocumentInPath("Project 1"));
        var definition = newAttributeDefinition(String.class, "status");
        var value1 = new AttributeValue(project, definition, "draft", 0);
        var value2 = new AttributeValue(project, definition, "final", 0);
        softly.assertThat(value1.compareTo(value2)).isEqualTo(0);
        softly.assertThat(value1.equals(value2)).isTrue();
    }

}

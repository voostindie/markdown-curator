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
    void toWeightedValue()
    {
        var project = new Project(vault.resolveDocumentInPath("Project 1"));
        var definition = newAttributeDefinition(Integer.class, "priority");
        var value = new AttributeValue(project, definition, 42, 100);
        var weightedValue = value.toWeightedValue();
        softly.assertThat(weightedValue.value()).isEqualTo(42);
        softly.assertThat(weightedValue.weight()).isEqualTo(100);
        softly.assertThat(weightedValue).isEqualTo(new WeightedValue(42, 100));
    }
}

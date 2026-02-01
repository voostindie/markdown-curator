package nl.ulso.curator.project;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SoftAssertionsExtension.class)
class AttributeDefinitionTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void newAttributeDefinition()
    {
        var definition = AttributeDefinition.newAttributeDefinition(String.class, "name");
        softly.assertThat(definition.valueType()).isEqualTo(String.class);
        softly.assertThat(definition.frontMatterProperty()).isEqualTo("name");
    }

    @Test
    void newAttributeDefinitionWithCustomFrontMatterFunction()
    {
        var definition = AttributeDefinition.newAttributeDefinition(String.class, "name",
            s -> ((String) s).toUpperCase()
        );
        assertThat(definition.asFrontMatterValue("bar")).isEqualTo("BAR");
    }

    @Test
    void invalidFrontMatterPropertyValueThrows()
    {
        var definition = AttributeDefinition.newAttributeDefinition(String.class, "name",
            s -> ((String) s).toUpperCase()
        );
        assertThatThrownBy(() -> definition.asFrontMatterValue(42))
            .isInstanceOf(IllegalArgumentException.class);
    }

}

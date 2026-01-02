package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.Change;
import nl.ulso.markdown_curator.ChangeProcessor;

import java.util.Objects;

/// Represents a single attribute value of a project. Objects of this type are to be published by
/// [ChangeProcessor]s as [Change]s. The [AttributeRegistry] tracks these and provides actual
/// values to other models and queries.
///
/// An attribute value has a weight. In case there are multiple producers of the same attribute
/// value, the system uses the value with the highest weight.
///
/// Attribute values are considered the same if they are for the same project and attribute
/// definition and have the same weight.
public record AttributeValue(
    Project project, AttributeDefinition definition, Object value, int weight)
    implements Comparable<AttributeValue>
{
    public AttributeValue
    {
        if (value != null && !definition.valueType().isInstance(value))
        {
            throw new IllegalArgumentException(
                "Value of type " + value.getClass().getSimpleName() +
                " cannot be used for attribute definition with type " +
                definition.valueType().getSimpleName());
        }
    }

    @Override
    public int compareTo(AttributeValue other)
    {
        requireSameProjectAndDefinition(other);
        return Integer.compare(other.weight, weight); // Higher comes before lower weights!
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null || getClass() != object.getClass())
        {
            return false;
        }
        AttributeValue other = (AttributeValue) object;
        requireSameProjectAndDefinition(other);
        return this.weight == other.weight;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(project, definition, weight);
    }

    private void requireSameProjectAndDefinition(AttributeValue other)
    {
        if (!project.equals(other.project))
        {
            throw new IllegalArgumentException(
                "Cannot compare attribute values of different projects");

        }
        if (!definition.equals(other.definition))
        {
            throw new IllegalArgumentException(
                "Cannot compare values of different attributes definitions");
        }
    }
}

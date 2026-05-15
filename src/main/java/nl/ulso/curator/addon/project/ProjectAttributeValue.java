package nl.ulso.curator.addon.project;

import nl.ulso.curator.change.Change;
import nl.ulso.curator.change.ChangeProcessor;

import java.util.Objects;

/// Represents a single attribute value for a project. Objects of this type are to be published by
/// [ChangeProcessor]s as [Change]s. The [ProjectAttributeRepository] tracks these and provides actual values
/// to other models and queries.
///
/// An attribute value has a weight. In case there are multiple producers of the same attribute
/// value, the system uses the value with the highest weight.
///
/// Attribute values are considered the same if they are for the same project and attribute
/// definition and have the same weight; the actual value is ignored.
public record ProjectAttributeValue(
    Project project, ProjectAttributeDefinition definition, Object value, int weight)
{
    public ProjectAttributeValue
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
    public boolean equals(Object object)
    {
        if (object == null || getClass() != object.getClass())
        {
            return false;
        }
        ProjectAttributeValue other = (ProjectAttributeValue) object;
        requireSameProjectAndDefinition(other);
        return this.weight == other.weight;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(project, definition, weight);
    }

    private void requireSameProjectAndDefinition(ProjectAttributeValue other)
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

    WeightedValue toWeightedValue()
    {
        return new WeightedValue(value, weight);
    }

    @Override
    public String toString()
    {
        return project().name() + "', " + definition.frontMatterProperty() + ": '" + value + "', weight: '" + weight;
    }
}

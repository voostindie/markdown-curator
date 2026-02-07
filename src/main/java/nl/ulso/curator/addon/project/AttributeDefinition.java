package nl.ulso.curator.addon.project;

import java.util.function.UnaryOperator;

/// Defines a pluggable project attribute of a certain type.
///
/// Project attributes have several benefits. They are:
///
/// - Efficient: their values are resolved exactly once per query run.
/// - Pluggable: they can be extended; modules can add their own properties to the system.
/// - Extensible: their values can be resolved in more than one way.
/// - Persistent: values are stored in the front matter of the underlying project documents.
///
/// The [ProjectModule] provides the following default attribute definitions:
///
/// - [#LAST_MODIFIED].
/// - [#LEAD]
/// - [#PRIORITY].
/// - [#STATUS].
///
/// @see [ProjectModule]
public interface AttributeDefinition
{
    String LAST_MODIFIED = "last_modified";
    String LEAD = "lead";
    String PRIORITY = "priority";
    String STATUS = "status";

    /// Defines a new attribute definition.
    ///
    /// The intended use is to call this method from a provider method in a Dagger module.
    ///
    /// **Important**: an attribute definition alone is meaningless. It also needs models that
    /// _produce_ attribute values.
    ///
    /// @see ProjectModule
    static AttributeDefinition newAttributeDefinition(
        Class<?> valueType, String frontMatterProperty)
    {
        return new AttributeDefinitionImpl(valueType, frontMatterProperty);
    }

    /// Defines a new attribute definition with a custom function to convert its values to front
    /// matter property values.
    ///
    /// @see #newAttributeDefinition(Class, String)
    static AttributeDefinition newAttributeDefinition(
        Class<?> valueType,
        String frontMatterProperty,
        UnaryOperator<Object> asFrontMatterFunction)
    {
        return new AttributeDefinitionImpl(valueType, frontMatterProperty, asFrontMatterFunction);
    }

    /// @return the type of the property value.
    Class<?> valueType();

    /// @return the name of the front matter property this property maps to.
    String frontMatterProperty();

    /// @return the value of the attribute in a proper format for the associated front matter
    /// property.
    Object asFrontMatterValue(Object value);
}

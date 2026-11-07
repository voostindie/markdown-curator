package nl.ulso.curator.addon.project;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
/// Attribute definitions are an extension point: extensions contribute them through
/// [nl.ulso.curator.extension.ExtensionFactory#contributions(nl.ulso.dictionary.Dictionary)], and
/// collect them through [nl.ulso.curator.extension.Services#all(Class)]. The project extension
/// contributes the following default attribute definitions:
///
/// - [#LAST_MODIFIED].
/// - [#LEAD]
/// - [#PRIORITY].
/// - [#STATUS].
///
/// @see ProjectExtensionFactory
public interface ProjectAttributeDefinition
{
    String LAST_MODIFIED = "last_modified";
    String LEAD = "lead";
    String PRIORITY = "priority";
    String STATUS = "status";

    /// Defines a new attribute definition.
    ///
    /// The intended use is to call this method from
    /// [nl.ulso.curator.extension.ExtensionFactory#contributions(nl.ulso.dictionary.Dictionary)].
    ///
    /// **Important**: an attribute definition alone is meaningless. It also needs models that
    /// _produce_ attribute values.
    ///
    /// @see ProjectExtensionFactory
    static ProjectAttributeDefinition newAttributeDefinition(
        Class<?> valueType, String frontMatterProperty)
    {
        return new DefaultProjectAttributeDefinition(valueType, frontMatterProperty);
    }

    /// Defines a new attribute definition with a custom function to convert its values to front
    /// matter property values.
    ///
    /// @see #newAttributeDefinition(Class, String)
    static ProjectAttributeDefinition newAttributeDefinition(
        Class<?> valueType,
        String frontMatterProperty,
        UnaryOperator<Object> asFrontMatterFunction)
    {
        return new DefaultProjectAttributeDefinition(valueType, frontMatterProperty, asFrontMatterFunction);
    }

    /// Turns a collection of attribute definitions, typically all contributions, into a map, keyed
    /// on their front matter property.
    ///
    /// @throws IllegalStateException if two definitions have the same front matter property.
    static Map<String, ProjectAttributeDefinition> byFrontMatterProperty(
        Collection<ProjectAttributeDefinition> definitions)
    {
        var map = new HashMap<String, ProjectAttributeDefinition>();
        for (var definition : definitions)
        {
            if (map.putIfAbsent(definition.frontMatterProperty(), definition) != null)
            {
                throw new IllegalStateException("Duplicate project attribute definition: " +
                                                definition.frontMatterProperty());
            }
        }
        return Map.copyOf(map);
    }

    /// @return the type of the property value.
    Class<?> valueType();

    /// @return the name of the front matter property this property maps to.
    String frontMatterProperty();

    /// @return the value of the attribute in a proper format for the associated front matter
    /// property.
    Object asFrontMatterValue(Object value);
}

package nl.ulso.markdown_curator.project;

import java.util.function.UnaryOperator;

/// Defines a pluggable project property of a certain type.
///
/// Project properties have several benefits. They are:
///
/// - Efficient: their values are resolved exactly once per query run.
/// - Pluggable: they can be extended; modules can add their own properties to the system.
/// - Extensible: their values can be resolved in more than one way.
/// - Persistent: values are stored in the front matter of the underlying project documents.
///
/// The following project properties are available by default, with their values extracted from a
/// front matter property:
///
/// - [#LAST_MODIFIED]: the last modification date of the project, a [java.time.LocalDate].
/// - [#LEAD]: the lead on the project, a [nl.ulso.markdown_curator.vault.Document].
/// - [#PRIORITY]: the priority of the project, an [Integer].
/// - [#STATUS]: the status of the project, a [String].
///
public interface ProjectProperty
{
    String LAST_MODIFIED = "last_modified";
    String LEAD = "lead";
    String PRIORITY = "priority";
    String STATUS = "status";

    /// Defines a new project property.
    ///
    /// The intended use is to call this method from a provider method in a Dagger module:
    ///
    /// ```java
    /// @Provides @Singleton @IntoSet @StringKey("special")
    /// ProjectProperty provideSpecialProjectProperty()
    /// {
    ///     return newProperty(String.class, "special");
    /// }
    /// ```
    ///
    /// **Important**: a property alone is not enough. It also needs at least one [ValueResolver].
    ///
    /// @see ProjectModule
    /// @see ValueResolver
    static ProjectProperty newProperty(Class<?> valueType, String frontMatterProperty)
    {
        return new ProjectPropertyImpl(valueType, frontMatterProperty);
    }

    /// Defines a new project property with a custom function to convert its value to a front matter
    /// property value.
    ///
    /// @see #newProperty(Class, String)
    static ProjectProperty newProperty(
        Class<?> valueType, String frontMatterProperty,
        UnaryOperator<Object> asFrontMatterFunction)
    {
        return new ProjectPropertyImpl(valueType, frontMatterProperty, asFrontMatterFunction);
    }

    /// @return the type of the property value.
    Class<?> valueType();

    /// @return the name of the front matter property this property maps to.
    String frontMatterProperty();

    /// @return the value of the property in a proper format for the associated front matter
    /// property.
    Object asFrontMatterValue(Object value);
}

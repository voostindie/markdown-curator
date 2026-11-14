package nl.ulso.curator.extension;

import static java.util.Objects.requireNonNull;

/// A point where any number of extensions - and the curator itself - can plug in values, following
/// the whiteboard pattern: the owner of the extension point doesn't look for contributors;
/// contributors publish their [Contribution]s, and the owner collects all of them through
/// [ExtensionContext#contributionsTo(ExtensionPoint)].
///
/// The owner declares an extension point as a constant, for others to contribute to:
///
/// ```java
/// ExtensionPoint<ProjectAttributeDefinition> PROJECT_ATTRIBUTES =
///     extensionPoint("project attributes", ProjectAttributeDefinition.class);
/// ```
///
/// Contributors create contributions from it, in
/// [ExtensionFactory#contributions(nl.ulso.dictionary.Dictionary)]:
///
/// ```java
/// return List.of(PROJECT_ATTRIBUTES.contribute(myAttributeDefinition));
/// ```
///
/// Unlike services, there can be any number of contributions to an extension point, and
/// contributions don't determine the order in which extensions are created. That's because
/// contributions are collected before any extension is created: they are simple values that don't
/// depend on anything. If a value needs to be injected with dependencies, make it a service instead.
///
/// Extension points are equal if their names and types are equal.
///
/// @param name the name of the extension point, for humans.
/// @param type the type of the values contributed to the extension point.
/// @param <T>  the type of the values contributed to the extension point.
public record ExtensionPoint<T>(String name, Class<T> type)
{
    public ExtensionPoint
    {
        requireNonNull(name);
        requireNonNull(type);
    }

    public static <T> ExtensionPoint<T> extensionPoint(String name, Class<T> type)
    {
        return new ExtensionPoint<>(name, type);
    }

    /// @param value the value to contribute to this extension point.
    /// @return a contribution of the value to this extension point.
    public Contribution contribute(T value)
    {
        return new Contribution(this, value);
    }

    @Override
    public String toString()
    {
        return "extension point '" + name + "' (" + type.getName() + ")";
    }
}

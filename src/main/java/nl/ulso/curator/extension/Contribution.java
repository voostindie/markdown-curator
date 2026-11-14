package nl.ulso.curator.extension;

import static java.util.Objects.requireNonNull;

/// A contribution to an extension point, following the whiteboard pattern: the owner of an
/// extension point doesn't look for contributors; contributors publish their contributions, and
/// the owner collects all of them through [ServiceRegistry#all(Class)].
///
/// Unlike services, there can be any number of contributions of a type, and contributions don't
/// determine the order in which extensions are loaded. That's because contributions are collected
/// before any extension is created: they are simple values that don't depend on anything. If a
/// contribution needs to be injected with dependencies, make it a service instead.
///
/// The curator contributes by binding contributions into an (unqualified) set; extensions
/// contribute through [ExtensionFactory#contributions(nl.ulso.dictionary.Dictionary)].
///
/// @param type  the type of the extension point.
/// @param value the contribution.
public record Contribution(Class<?> type, Object value)
{
    public Contribution
    {
        requireNonNull(type);
        requireNonNull(value);
        if (!type.isInstance(value))
        {
            throw new IllegalArgumentException(
                "Contribution is not an instance of " + type.getName() + ": " + value);
        }
    }

    public static <T> Contribution contribution(Class<T> type, T value)
    {
        return new Contribution(type, value);
    }
}

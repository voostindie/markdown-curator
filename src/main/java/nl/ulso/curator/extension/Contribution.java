package nl.ulso.curator.extension;

import static java.util.Objects.requireNonNull;

/// A value contributed to an [ExtensionPoint]. Create contributions through
/// [ExtensionPoint#contribute(Object)].
///
/// The curator contributes by binding contributions into an (unqualified) set; extensions
/// contribute through [ExtensionFactory#contributions(nl.ulso.dictionary.Dictionary)].
///
/// @param extensionPoint the extension point contributed to.
/// @param value          the contributed value.
public record Contribution(ExtensionPoint<?> extensionPoint, Object value)
{
    public Contribution
    {
        requireNonNull(extensionPoint);
        requireNonNull(value);
        if (!extensionPoint.type().isInstance(value))
        {
            throw new IllegalArgumentException(
                "Contribution to " + extensionPoint + " is not an instance of " +
                extensionPoint.type().getName() + ": " + value);
        }
    }
}

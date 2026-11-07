package nl.ulso.curator.extension;

import static java.util.Objects.requireNonNull;

/// A service published by an [Extension], or by the curator itself, for other extensions to
/// require.
///
/// There can be only one provider of a service type. Extensions declare the services they provide
/// and require in their [ExtensionFactory]; the curator uses these to load extensions in the right
/// order, and to report missing services. The curator publishes services by binding
/// registrations into an (unqualified) set; extensions bind them into a set qualified with
/// [ExtensionBinding].
///
/// @param type     the type of the service; this is the key other extensions look it up with.
/// @param instance the service itself.
public record ServiceRegistration(Class<?> type, Object instance)
{
    public ServiceRegistration
    {
        requireNonNull(type);
        requireNonNull(instance);
        if (!type.isInstance(instance))
        {
            throw new IllegalArgumentException(
                "Service is not an instance of " + type.getName() + ": " + instance);
        }
    }

    public static <T> ServiceRegistration service(Class<T> type, T instance)
    {
        return new ServiceRegistration(type, instance);
    }
}

package nl.ulso.markdown_curator.project;

import java.util.Optional;

/// Resolver for [ProjectProperty] values; every property needs at least one resolver to extract its
/// value from the [nl.ulso.markdown_curator.vault.Vault], but it can have more, in which case they
/// are ordered on priority.
///
/// To resolve a value for a project property, all available resolvers are tried in order, until a
/// value is resolved.
public interface ValueResolver
{
    /// @return the property resolves values for.
    ProjectProperty property();

    /// Resolves the value for the project property this resolver is for.
    ///
    /// @param project Project to resolve the property value for.
    /// @return optionally resolved value.
    Optional<?> from(Project project);

    /// @return Order (priority) of this resolver.
    int order();
}

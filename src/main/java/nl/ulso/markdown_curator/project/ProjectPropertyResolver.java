package nl.ulso.markdown_curator.project;

import java.util.Optional;

/**
 * Resolver for {@link ProjectProperty} values; every property needs at least one resolver to
 * extract its value from the {@link nl.ulso.markdown_curator.vault.Vault}, but it can have more,
 * in which case they are ordered on priority.
 * <p/>
 * To resolve a value for a project property, all available resolvers are tried in order, until
 * a value is resolved.
 */
public interface ProjectPropertyResolver
{
    /**
     * @return the {@link ProjectProperty} this is a resolver for.
     */
    ProjectProperty projectProperty();

    /**
     * @param project Project to resolve the property value for.
     * @return optional value resolved for this resolver's project property.
     */
    Optional<?> resolveValue(Project project);

    /**
     * @return Order (priority) of this resolver.
     */
    int order();
}

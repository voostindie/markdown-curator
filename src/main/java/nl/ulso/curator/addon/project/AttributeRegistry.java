package nl.ulso.curator.addon.project;

import nl.ulso.curator.change.ChangeProcessor;

import java.util.Collection;
import java.util.Optional;

/// Registry of all custom project attribute values.
///
/// The set of available attributes is customizable. 4 are provided by default. See
/// [AttributeDefinition] for the complete list.
///
/// The values of project attributes can be produced by several [ChangeProcessor]s. Each different
/// provider gives a _weight_ to the values it produces. The registry selects the value with the
/// largest weight. See [AttributeValue] for more information.
///
/// Values are not only made available to other parts of the system through this registry, they are
/// also persisted in the front matter of the underlying project documents. See
/// [FrontMatterAttributeProducer].
///
/// The registry is fully updated when a [AttributeRegistryUpdate] is published on the changelog.
/// Change processors that depend on this registry should consume that object to ensure it comes
/// after.
///
/// The registry guarantees that it covers all projects in the [ProjectRepository] and no others.
public interface AttributeRegistry
{
    /// @return all attribute definitions managed by this registry.
    Collection<AttributeDefinition> attributeDefinitions();

    /// @return the value of a named attribute from a project.
    /// @throws NullPointerException if the project does not exist in the [ProjectRepository].
    /// @throws NullPointerException if the attribute with the given name unknown to the registry.
    Optional<?> valueOf(Project project, String attributeName);

    /// @return the value of an attribute definition from a project.
    /// @throws NullPointerException if the project does not exist in the [ProjectRepository].
    Optional<?> valueOf(Project project, AttributeDefinition definition);
}

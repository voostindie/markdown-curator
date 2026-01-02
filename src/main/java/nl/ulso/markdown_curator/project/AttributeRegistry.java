package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.ChangeProcessor;
import nl.ulso.markdown_curator.vault.Document;

import java.util.*;

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
public interface AttributeRegistry
{
    /// @return All attribute definitions managed by this registry.
    Collection<AttributeDefinition> attributeDefinitions();

    /// @return All projects known by this registry; these are guaranteed to be the same as the ones
    /// managed by the [ProjectRepository].
    Set<Project> projects();

    /// @return The project matching the provided document, if any.
    Optional<Project> projectFor(Document document);

    /// @return the value of the attribute with the
    Optional<?> attributeValue(Project project, String attributeName);

    Optional<?> attributeValue(Project project, AttributeDefinition definition);
}

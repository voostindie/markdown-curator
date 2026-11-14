package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import nl.ulso.curator.extension.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;

import java.time.LocalDate;
import java.util.*;

import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.*;
import static nl.ulso.curator.extension.Contribution.contribution;

/// Creates the project extension:
///
/// ```yaml
/// extensions:
///   project:
///     folder: Projects
/// ```
///
/// The extension contributes the default [ProjectAttributeDefinition]s:
///
/// - [ProjectAttributeDefinition#LAST_MODIFIED]: the last modification date of the project, a
/// [LocalDate].
/// - [ProjectAttributeDefinition#LEAD]: the lead on the project, a [Document].
/// - [ProjectAttributeDefinition#PRIORITY]: the priority of the project, an [Integer].
/// - [ProjectAttributeDefinition#STATUS]: the status of the project, a [String].
///
/// To add an attribute, contribute a [ProjectAttributeDefinition] with a unique front matter
/// property.
final class ProjectExtensionFactory
    implements ExtensionFactory
{
    static final String FOLDER = "folder";

    private final ProjectExtension.Factory factory;

    @Inject
    ProjectExtensionFactory(ProjectExtension.Factory factory)
    {
        this.factory = factory;
    }

    @Override
    public String description()
    {
        return "Projects, one document per project, with pluggable attributes.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(FOLDER, "Folder with project documents; defaults to `Projects`.");
    }

    @Override
    public Set<Class<?>> provides()
    {
        return Set.of(ProjectRepository.class, ProjectAttributeRepository.class);
    }

    @Override
    public List<Contribution> contributions(Dictionary configuration)
    {
        return List.of(
            contribution(ProjectAttributeDefinition.class,
                newAttributeDefinition(LocalDate.class, LAST_MODIFIED, Object::toString)),
            contribution(ProjectAttributeDefinition.class,
                newAttributeDefinition(Document.class, LEAD, d -> ((Document) d).link())),
            contribution(ProjectAttributeDefinition.class,
                newAttributeDefinition(Integer.class, PRIORITY)),
            contribution(ProjectAttributeDefinition.class,
                newAttributeDefinition(String.class, STATUS))
        );
    }

    @Override
    public Extension create(Dictionary configuration, ServiceRegistry serviceRegistry)
    {
        return factory.create(
            new ProjectSettings(configuration.string(FOLDER, "Projects")),
            byFrontMatterProperty(serviceRegistry.all(ProjectAttributeDefinition.class))
        );
    }
}

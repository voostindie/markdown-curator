package nl.ulso.curator.addon.project;

import nl.ulso.curator.vault.Document;

import java.util.*;

final class ProjectAttributeRepositoryStub
    implements ProjectAttributeRepository
{
    private final Map<Project, Map<ProjectAttributeDefinition, Object>> attributes =
        new HashMap<>();

    ProjectAttributeRepositoryStub withAttribute(
        Document document, String attributeName, Object value)
    {
        attributes.computeIfAbsent(new Project(document), _ -> new HashMap<>())
            .put(ProjectTestData.ATTRIBUTE_DEFINITIONS.get(attributeName), value);
        return this;
    }

    @Override
    public Collection<ProjectAttributeDefinition> attributeDefinitions()
    {
        return ProjectTestData.ATTRIBUTE_DEFINITIONS.values();
    }

    @Override
    public Optional<?> valueOf(Project project, String attributeName)
    {
        return valueOf(project, ProjectTestData.ATTRIBUTE_DEFINITIONS.get(attributeName));
    }

    @Override
    public Optional<?> valueOf(Project project, ProjectAttributeDefinition definition)
    {
        return Optional.ofNullable(attributes.get(project).get(definition));
    }
}

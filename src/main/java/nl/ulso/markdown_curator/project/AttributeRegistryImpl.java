package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.*;
import nl.ulso.markdown_curator.vault.Document;

import java.util.*;

import static java.util.Collections.emptyList;

@Singleton
final class AttributeRegistryImpl
    extends DataModelTemplate
    implements AttributeRegistry
{
    private final Map<String, AttributeDefinition> attributeDefinitions;
    private final Map<Project, Map<AttributeDefinition, SortedSet<AttributeValue>>>
        projectAttributes;

    @Inject
    AttributeRegistryImpl(Map<String, AttributeDefinition> attributeDefinitions)
    {
        this.attributeDefinitions = attributeDefinitions;
        this.projectAttributes = new HashMap<>();
        registerChangeHandler(
            hasObjectType(Project.class).and(isDeletion()),
            this::processProjectDeletion
        );
        registerChangeHandler(
            hasObjectType(AttributeValue.class).and(isCreation().or(isModification())),
            this::processAttributeValueChange
        );
        registerChangeHandler(
            hasObjectType(AttributeValue.class).and(isDeletion()),
            this::processAttributeValueDeletion
        );
    }

    private Collection<Change<?>> processProjectDeletion(Change<?> change)
    {
        projectAttributes.remove(change.objectAs(Project.class));
        return emptyList();
    }

    private Collection<Change<?>> processAttributeValueChange(Change<?> change)
    {
        var value = change.objectAs(AttributeValue.class);
        var attribute = projectAttributes.computeIfAbsent(
            value.project(), _ -> new HashMap<>()
        );
        var values = attribute.computeIfAbsent(
            value.definition(), _ -> new TreeSet<>());
        values.remove(value);
        values.add(value);
        return emptyList();
    }

    private Collection<Change<?>> processAttributeValueDeletion(Change<?> change)
    {
        var value = change.objectAs(AttributeValue.class);
        var attribute = projectAttributes.get(value.project());
        if (attribute != null)
        {
            var values = attribute.get(value.definition());
            if (values != null)
            {
                values.remove(value);
            }
        }
        return emptyList();
    }

    @Override
    protected boolean isFullRefreshRequired(Changelog changelog)
    {
        return false;
    }

    @Override
    public Collection<Change<?>> fullRefresh()
    {
        throw new IllegalStateException("This method should never be called!");
    }

    @Override
    public Set<Class<?>> consumedObjectTypes()
    {
        return Set.of(Project.class, AttributeValue.class);
    }

    @Override
    public Set<Project> projects()
    {
        return projectAttributes.keySet();
    }

    @Override
    public Collection<AttributeDefinition> attributeDefinitions()
    {
        return attributeDefinitions.values();
    }

    @Override
    public Optional<?> attributeValue(Project project, String attributeName)
    {
        return attributeValue(project, attributeDefinitions.get(attributeName));
    }

    @Override
    public Optional<?> attributeValue(Project project, AttributeDefinition definition)
    {
        var set = projectAttributes.get(project).get(definition);
        if (set == null || set.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(set.first().value());
    }

    @Override
    public Optional<Project> projectFor(Document document)
    {
        return projectAttributes.keySet().stream()
            .filter(project -> project.document().name().equals(document.name()))
            .findFirst();
    }
}

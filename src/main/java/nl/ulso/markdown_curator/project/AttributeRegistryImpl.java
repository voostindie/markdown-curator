package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.*;

import java.util.*;

import static java.util.HashSet.newHashSet;
import static nl.ulso.markdown_curator.Change.isCreateOrUpdate;
import static nl.ulso.markdown_curator.Change.isDelete;
import static nl.ulso.markdown_curator.Change.isPayloadType;
import static nl.ulso.markdown_curator.project.AttributeRegistryUpdate.REGISTRY_CHANGE;

@Singleton
final class AttributeRegistryImpl
    extends ChangeProcessorTemplate
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
            isPayloadType(Project.class).and(isDelete()),
            this::processProjectDeletion
        );
        registerChangeHandler(
            isPayloadType(AttributeValue.class).and(isCreateOrUpdate()),
            this::processAttributeValueChange
        );
        registerChangeHandler(
            isPayloadType(AttributeValue.class).and(isDelete()),
            this::processAttributeValueDeletion
        );
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Project.class, AttributeValue.class);
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(AttributeRegistryUpdate.class);
    }

    private Collection<Change<?>> processProjectDeletion(Change<?> change)
    {
        projectAttributes.remove(change.as(Project.class).value());
        return REGISTRY_CHANGE;
    }

    private Collection<Change<?>> processAttributeValueChange(Change<?> change)
    {
        var value = change.as(AttributeValue.class).value();
        var attribute = projectAttributes.computeIfAbsent(
            value.project(), _ -> new HashMap<>()
        );
        var values = attribute.computeIfAbsent(
            value.definition(), _ -> new TreeSet<>());
        values.remove(value);
        values.add(value);
        return REGISTRY_CHANGE;
    }

    private Collection<Change<?>> processAttributeValueDeletion(Change<?> change)
    {
        var value = change.as(AttributeValue.class).value();
        var attribute = projectAttributes.get(value.project());
        if (attribute != null)
        {
            var values = attribute.get(value.definition());
            if (values != null)
            {
                values.remove(value);
            }
        }
        return REGISTRY_CHANGE;
    }

    /// Collect the changes in a set instead of a list, so that at the end of the run there is
    /// exactly one change in the changelog.
    @Override
    protected Collection<Change<?>> createChangeCollection()
    {
        return newHashSet(1);
    }

    @Override
    protected boolean isFullRefreshRequired(Changelog changelog)
    {
        return false;
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
        var attribute = projectAttributes.get(project);
        if (attribute == null)
        {
            return Optional.empty();
        }
        var set = attribute.get(definition);
        if (set == null || set.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.ofNullable(set.first().value());
    }
}

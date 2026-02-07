package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;

import java.util.*;

import static java.util.HashSet.newHashSet;
import static nl.ulso.curator.addon.project.AttributeRegistryUpdate.REGISTRY_CHANGE;
import static nl.ulso.curator.change.Change.isCreate;
import static nl.ulso.curator.change.Change.isCreateOrUpdate;
import static nl.ulso.curator.change.Change.isDelete;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

@Singleton
final class DefaultAttributeRegistry
    extends ChangeProcessorTemplate
    implements AttributeRegistry
{
    private final Map<String, AttributeDefinition> attributeDefinitions;
    private final Map<String, Map<AttributeDefinition, SortedSet<WeightedValue>>>
        projectAttributes;

    @Inject
    DefaultAttributeRegistry(Map<String, AttributeDefinition> attributeDefinitions)
    {
        this.attributeDefinitions = attributeDefinitions;
        this.projectAttributes = new HashMap<>();
    }

    @Override
    protected Set<? extends ChangeHandler> createChangeHandlers()
    {
        return Set.of(
            newChangeHandler(
                isPayloadType(Project.class).and(isCreate()),
                this::projectCreated
            ),
            newChangeHandler(
                isPayloadType(Project.class).and(isDelete()),
                this::projectDeleted
            ),
            newChangeHandler(
                isPayloadType(AttributeValue.class).and(isCreateOrUpdate()),
                this::attributeValueCreatedOrUpdated
            ),
            newChangeHandler(
                isPayloadType(AttributeValue.class).and(isDelete()),
                this::attributeValueDeleted
            )
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

    /// Prepares the internal data structures for a new project.
    ///
    /// This might be a bit of a waste - late creation only when needed is more efficient - but it
    /// simplifies the rest of the code.
    private Collection<Change<?>> projectCreated(Change<?> change)
    {
        var project = change.as(Project.class).value();
        var attributeValues = new HashMap<AttributeDefinition, SortedSet<WeightedValue>>(
            attributeDefinitions.size());
        attributeDefinitions.forEach(
            (_, definition) -> attributeValues.put(definition, new TreeSet<>()));
        projectAttributes.put(project.name(), attributeValues);
        return REGISTRY_CHANGE;
    }

    private Collection<Change<?>> projectDeleted(Change<?> change)
    {
        var project = change.as(Project.class).value();
        projectAttributes.remove(project.name());
        return REGISTRY_CHANGE;
    }

    private Collection<Change<?>> attributeValueCreatedOrUpdated(Change<?> change)
    {
        var attributeValue = change.as(AttributeValue.class).value();
        var weightedValue = attributeValue.toWeightedValue();
        var projectAttributeValues = projectAttributes.get(attributeValue.project().name());
        var weightedValues = projectAttributeValues.get(attributeValue.definition());
        weightedValues.remove(weightedValue);
        weightedValues.add(weightedValue);
        return REGISTRY_CHANGE;
    }

    private Collection<Change<?>> attributeValueDeleted(Change<?> change)
    {
        var attributeValue = change.as(AttributeValue.class).value();
        var projectAttributeValues = projectAttributes.get(attributeValue.project().name());
        var weightedValues = projectAttributeValues.get(attributeValue.definition());
        weightedValues.remove(attributeValue.toWeightedValue());
        return REGISTRY_CHANGE;
    }

    /// Collect the changes in a set instead of a list so that at the end of the run there is
    /// exactly one change in the changelog.
    @Override
    protected Collection<Change<?>> createChangeCollection()
    {
        return newHashSet(1);
    }

    @Override
    protected boolean isResetRequired(Changelog changelog)
    {
        return false;
    }

    @Override
    public Collection<AttributeDefinition> attributeDefinitions()
    {
        return attributeDefinitions.values();
    }

    @Override
    public Optional<?> valueOf(Project project, String attributeName)
    {
        return valueOf(project, attributeDefinitions.get(attributeName));
    }

    @Override
    public Optional<?> valueOf(Project project, AttributeDefinition definition)
    {
        var weightedValues = projectAttributes.get(project.name()).get(definition);
        if (weightedValues.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(weightedValues.last().value());
    }
}

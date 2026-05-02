package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;

import java.util.*;

import static java.util.HashSet.newHashSet;
import static nl.ulso.curator.addon.project.ProjectAttributeRepositoryUpdate.REPOSITORY_UPDATE;
import static nl.ulso.curator.change.Change.isCreate;
import static nl.ulso.curator.change.Change.isCreateOrUpdate;
import static nl.ulso.curator.change.Change.isDelete;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

@Singleton
final class DefaultProjectAttributeRepository
    extends ChangeProcessorTemplate
    implements ProjectAttributeRepository
{
    private final Map<String, ProjectAttributeDefinition> attributeDefinitions;
    private final Map<String, Map<ProjectAttributeDefinition, SortedSet<WeightedValue>>>
        projectAttributes;

    @Inject
    DefaultProjectAttributeRepository(Map<String, ProjectAttributeDefinition> attributeDefinitions)
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
                isPayloadType(ProjectAttributeValue.class).and(isCreateOrUpdate()),
                this::attributeValueCreatedOrUpdated
            ),
            newChangeHandler(
                isPayloadType(ProjectAttributeValue.class).and(isDelete()),
                this::attributeValueDeleted
            )
        );
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Project.class, ProjectAttributeValue.class);
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(ProjectAttributeRepositoryUpdate.class);
    }

    /// Prepares the internal data structures for a new project.
    ///
    /// This might be a bit of a waste - late creation only when needed is more efficient - but it
    /// simplifies the rest of the code.
    private void projectCreated(Change<?> change, ChangeCollector collector)
    {
        var project = change.as(Project.class).value();
        var attributeValues = new HashMap<ProjectAttributeDefinition, SortedSet<WeightedValue>>(
            attributeDefinitions.size());
        attributeDefinitions.forEach(
            (_, definition) -> attributeValues.put(definition, new TreeSet<>()));
        projectAttributes.put(project.name(), attributeValues);
        collector.add(REPOSITORY_UPDATE);
    }

    private void projectDeleted(Change<?> change, ChangeCollector collector)
    {
        var project = change.as(Project.class).value();
        projectAttributes.remove(project.name());
        collector.add(REPOSITORY_UPDATE);
    }

    private void attributeValueCreatedOrUpdated(Change<?> change, ChangeCollector collector)
    {
        var attributeValue = change.as(ProjectAttributeValue.class).value();
        var projectAttributeValues = projectAttributes.get(attributeValue.project().name());
        if (projectAttributeValues == null)
        {
            return;
        }
        var weightedValues = projectAttributeValues.get(attributeValue.definition());
        var weightedValue = attributeValue.toWeightedValue();
        weightedValues.remove(weightedValue);
        weightedValues.add(weightedValue);
        collector.add(REPOSITORY_UPDATE);
    }

    private void attributeValueDeleted(Change<?> change, ChangeCollector collector)
    {
        var attributeValue = change.as(ProjectAttributeValue.class).value();
        var projectAttributeValues = projectAttributes.get(attributeValue.project().name());
        if (projectAttributeValues == null)
        {
            return;
        }
        var weightedValues = projectAttributeValues.get(attributeValue.definition());
        weightedValues.remove(attributeValue.toWeightedValue());
        collector.add(REPOSITORY_UPDATE);
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
    public Collection<ProjectAttributeDefinition> attributeDefinitions()
    {
        return attributeDefinitions.values();
    }

    @Override
    public Optional<?> valueOf(Project project, String attributeName)
    {
        return valueOf(project, attributeDefinitions.get(attributeName));
    }

    @Override
    public Optional<?> valueOf(Project project, ProjectAttributeDefinition definition)
    {
        var weightedValues = projectAttributes.get(project.name()).get(definition);
        if (weightedValues.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(weightedValues.last().value());
    }
}

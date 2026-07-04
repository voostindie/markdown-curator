package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;
import nl.ulso.curator.statistics.MeasurementCollector;
import nl.ulso.curator.statistics.MeasurementTracker;
import org.slf4j.Logger;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySortedSet;
import static java.util.HashMap.newHashMap;
import static java.util.HashSet.newHashSet;
import static java.util.Objects.requireNonNull;
import static nl.ulso.curator.addon.project.ProjectAttributeRepositoryUpdate.REPOSITORY_UPDATE;
import static nl.ulso.curator.change.Change.isCreate;
import static nl.ulso.curator.change.Change.isDelete;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.Change.isUpdate;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;
import static org.slf4j.LoggerFactory.getLogger;

/// [ChangeProcessor] that collects all [ProjectAttributeValue] events — those can come from
/// anywhere — and that stores them in an internal map; There can be multiple attribute values for a
/// single attribute definition, with different weights; the repository tracks them all in a sorted
/// set.
@Singleton
final class DefaultProjectAttributeRepository
    extends ChangeProcessorTemplate
    implements ProjectAttributeRepository, MeasurementTracker
{
    private static final Logger LOGGER = getLogger(DefaultProjectAttributeRepository.class);

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
    protected List<? extends ChangeHandler> createChangeHandlers()
    {
        return List.of(
            newChangeHandler(
                isPayloadType(Project.class).and(isDelete()),
                this::projectDeleted
            ),
            newChangeHandler(
                isPayloadType(ProjectAttributeValue.class).and(isCreate()),
                this::attributeValueCreated
            ),
            newChangeHandler(
                isPayloadType(ProjectAttributeValue.class).and(isUpdate()),
                this::attributeValueUpdated
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

    @Override
    public void reset()
    {
        projectAttributes.clear();
    }

    private void projectDeleted(Change<?> change, ChangeCollector collector)
    {
        var project = change.as(Project.class).oldValue();
        projectAttributes.remove(project.name());
    }

    private void attributeValueCreated(Change<?> change, ChangeCollector collector)
    {
        var newProjectAttributeValue = change.as(ProjectAttributeValue.class).value();
        var newWeightedValue = newProjectAttributeValue.toWeightedValue();
        var weightedValues = resolveWeightedValues(newProjectAttributeValue);
        if (weightedValues.contains(newWeightedValue))
        {
            if (LOGGER.isWarnEnabled())
            {
                LOGGER.warn(
                    "Unexpected existing weighted value for attribute '{}' with weight '{}' on " +
                    "project '{}' found.",
                    newProjectAttributeValue.definition().frontMatterProperty(),
                    newWeightedValue.weight(),
                    newProjectAttributeValue.project().name()
                );
            }
            weightedValues.remove(newWeightedValue);
        }
        weightedValues.add(newWeightedValue);
        collector.add(REPOSITORY_UPDATE);
    }

    private void attributeValueUpdated(Change<?> change, ChangeCollector collector)
    {
        var projectAttributeValue = change.as(ProjectAttributeValue.class).value();
        var newWeightedValue = projectAttributeValue.toWeightedValue();
        var weightedValues = resolveWeightedValues(projectAttributeValue);
        var oldWeightedValue = weightedValues.stream()
            .filter(value -> value.weight() == newWeightedValue.weight())
            .findFirst()
            .orElse(null);
        if (oldWeightedValue == null)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                    "Expected existing weighted value for attribute '{}' with weight '{}' on " +
                    "project '{}' not found.",
                    projectAttributeValue.definition().frontMatterProperty(),
                    newWeightedValue.weight(),
                    projectAttributeValue.project().name()
                );
            }
        }
        else
        {
            if (oldWeightedValue.value().equals(newWeightedValue.value()))
            {
                if (LOGGER.isWarnEnabled())
                {
                    LOGGER.warn(
                        "Detected meaningless value update for attribute '{}' with weight '{}' " +
                        "on project '{}'.",
                        projectAttributeValue.definition().frontMatterProperty(),
                        newWeightedValue.weight(),
                        projectAttributeValue.project().name()
                    );
                }
            }
            weightedValues.remove(oldWeightedValue);
        }
        weightedValues.add(newWeightedValue);
        collector.add(REPOSITORY_UPDATE);
    }

    private void attributeValueDeleted(Change<?> change, ChangeCollector collector)
    {
        var oldProjectAttributeValue = change.as(ProjectAttributeValue.class).value();
        var oldWeightedValue = oldProjectAttributeValue.toWeightedValue();
        var weightedValues = projectAttributes
            .getOrDefault(oldProjectAttributeValue.project().name(), emptyMap())
            .getOrDefault(oldProjectAttributeValue.definition(), emptySortedSet());
        if (!weightedValues.contains(oldWeightedValue))
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                    "Detected deletion of a non-existent value for attribute '{}' with weight " +
                    "'{}' on  project '{}'.",
                    oldProjectAttributeValue.definition().frontMatterProperty(),
                    oldWeightedValue.weight(),
                    oldProjectAttributeValue.project().name()
                );
            }
            return;
        }
        weightedValues.remove(oldWeightedValue);
        collector.add(REPOSITORY_UPDATE);
    }

    private SortedSet<WeightedValue> resolveWeightedValues(
        ProjectAttributeValue projectAttributeValue
    )
    {
        return projectAttributes.computeIfAbsent(
            projectAttributeValue.project().name(),
            _ -> newHashMap(attributeDefinitions.size())
        ).computeIfAbsent(
            projectAttributeValue.definition(),
            _ -> new TreeSet<>()
        );
    }

    /// Collect the changes in a set instead of a list so that at the end of the run there is
    /// exactly one change in the changelog.
    @Override
    public Collection<Change<?>> createChangeCollection()
    {
        return newHashSet(1);
    }

    @Override
    public Collection<ProjectAttributeDefinition> attributeDefinitions()
    {
        return attributeDefinitions.values();
    }

    @Override
    public Optional<?> valueOf(Project project, String attributeName)
    {
        return valueOf(project, requireNonNull(attributeDefinitions.get(attributeName)));
    }

    @Override
    public Optional<?> valueOf(Project project, ProjectAttributeDefinition definition)
    {
        var weightedValues = projectAttributes
            .getOrDefault(project.name(), emptyMap())
            .getOrDefault(definition, emptySortedSet());
        if (weightedValues.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(weightedValues.last().value());
    }

    @Override
    public String name()
    {
        return ProjectAttributeRepository.class.getSimpleName();
    }

    @Override
    public void collectMeasurements(MeasurementCollector collector)
    {
        collector.total(ProjectAttributeDefinition.class, attributeDefinitions.size());
        collector.total(ProjectAttributeValue.class, projectAttributes.size());
        collector.total(WeightedValue.class, projectAttributes.values().stream()
            .flatMap(map -> map.values().stream())
            .mapToLong(Collection::size)
            .sum()
        );
    }
}

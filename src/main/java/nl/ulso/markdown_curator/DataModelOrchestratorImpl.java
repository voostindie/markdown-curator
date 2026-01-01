package nl.ulso.markdown_curator;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.vault.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.List.copyOf;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
final class DataModelOrchestratorImpl
    implements DataModelOrchestrator
{
    private static final Set<Class<?>> RESERVED_OBJECT_TYPES =
        Set.of(Vault.class, Folder.class, Document.class);

    private static final Logger LOGGER = getLogger(DataModelOrchestratorImpl.class);

    private final List<DataModel> dataModels;

    @Inject
    DataModelOrchestratorImpl(Set<DataModel> dataModels)
    {
        if (producesAnyOf(dataModels, RESERVED_OBJECT_TYPES))
        {
            throw new IllegalArgumentException(
                "Data models may not produce any of the reserved object types: " +
                RESERVED_OBJECT_TYPES.stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "))
            );
        }
        this.dataModels = orderDataModels(dataModels);
    }

    List<DataModel> dataModels()
    {
        return dataModels;
    }

    /// Order the available set of data models in accordance to the requirements of the
    /// orchestrator: prodducers before consumers, dependencies before dependents.
    private List<DataModel> orderDataModels(Set<DataModel> dataModels)
    {
        var availableObjectTypes = new HashSet<>(RESERVED_OBJECT_TYPES);
        var queue = new LinkedList<>(dataModels);
        var size = dataModels.size();
        var maxIterations = (size * (size + 1)) / 2;
        int iteration = 0;
        var result = new ArrayList<DataModel>(size);
        while (!queue.isEmpty())
        {
            iteration++;
            if (iteration > maxIterations)
            {
                throw new IllegalStateException(
                    "Dependency cycle across data models detected, or an unsatisfied consumer.");
            }
            var model = queue.pollFirst();
            if (!availableObjectTypes.containsAll(model.consumedObjectTypes()))
            {
                queue.addLast(model);
                continue;
            }
            if (producesAnyOf(queue, model.consumedObjectTypes()))
            {
                queue.addLast(model);
                continue;
            }
            if (!result.containsAll(model.dependentModels()))
            {
                queue.addLast(model);
                continue;
            }
            result.add(model);
            availableObjectTypes.addAll(model.producedObjectTypes());
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("{} data models will be refreshed in this order: {}", result.size(),
                result.stream().map(model -> model.getClass().getSimpleName())
                    .collect(Collectors.joining(", "))
            );
        }
        return copyOf(result);
    }

    /// Checks if any of the data models in the collection produce any of the given object types.
    private boolean producesAnyOf(
        Collection<DataModel> dataModels, Set<Class<?>> objectTypes)
    {
        return dataModels.stream().anyMatch(model ->
            objectTypes.stream().anyMatch(objectType ->
                model.producedObjectTypes().contains(objectType)));
    }

    public Changelog refreshAllDataModels(Change<?> change)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Refreshing {} data model(s)", dataModels.size());
        }
        var changelog = changelogFor(change);
        for (DataModel model : dataModels)
        {
            try
            {
                var filteredChangelog = changelog.changelogFor(model.consumedObjectTypes());
                if (filteredChangelog.isEmpty())
                {
                    LOGGER.debug("No relevant changes for data model {} available. Skipping.",
                        model.getClass().getSimpleName()
                    );
                    continue;
                }
                var newChangelog = model.process(filteredChangelog);
                verifyChanges(model, newChangelog);
                changelog = changelog.append(newChangelog);
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Refreshed data model {}", model.getClass().getSimpleName());
                }
            }
            catch (RuntimeException e)
            {
                LOGGER.error("Caught runtime exception while refreshing data model {}",
                    model.getClass().getSimpleName(), e
                );
            }
        }
        return changelog;
    }

    private static void verifyChanges(DataModel model, Changelog newChangelog)
    {
        if (newChangelog.changes()
            .anyMatch(c -> !model.producedObjectTypes().contains(c.objectType())))
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                    "Data model {} produced changes of a type that it doesn't claim to " +
                    "produce. Produced types: {}. Allowed types: {}.",
                    model.getClass().getSimpleName(),
                    newChangelog.changes()
                        .map(Change::objectType)
                        .map(Class::getSimpleName)
                        .collect(Collectors.toSet()),
                    model.producedObjectTypes().stream()
                        .map(Class::getSimpleName)
                        .collect(Collectors.toSet())
                );
            }
            throw new IllegalStateException(
                "Data model " + model.getClass().getSimpleName() +
                " is producing changes of a type that it doesn't claim to produce.");
        }
    }
}

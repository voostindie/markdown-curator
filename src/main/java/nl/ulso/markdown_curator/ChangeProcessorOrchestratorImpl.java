package nl.ulso.markdown_curator;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.vault.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.toSet;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
final class ChangeProcessorOrchestratorImpl
    implements ChangeProcessorOrchestrator
{
    private static final Set<Class<?>> RESERVED_OBJECT_TYPES =
        Set.of(Vault.class, Folder.class, Document.class);

    private static final Logger LOGGER = getLogger(ChangeProcessorOrchestratorImpl.class);

    private final List<ChangeProcessor> changeProcessors;

    @Inject
    ChangeProcessorOrchestratorImpl(
        Set<ChangeProcessor> changeProcessors,
        @ExternalChangeObjectType Set<Class<?>> externalObjectTypes)
    {
        verifyReservedObjectTypeProducers(changeProcessors);
        this.changeProcessors = orderChangeProcessors(changeProcessors, externalObjectTypes);
    }

    ChangeProcessorOrchestratorImpl(Set<ChangeProcessor> changeProcessors)
    {
        this(changeProcessors, emptySet());
    }

    /// Verify that none of the provided change processors produce object types that are reserved
    /// by the core system.
    private void verifyReservedObjectTypeProducers(Set<ChangeProcessor> changeProcessors)
    {
        var processorsToVerify = changeProcessors.stream()
            .filter(processor -> !processor.getClass().equals(VaultReloader.class))
            .collect(toSet());
        if (producesAnyOf(processorsToVerify, RESERVED_OBJECT_TYPES))
        {
            throw new IllegalArgumentException(
                "Change processors may not produce any of the reserved object types: " +
                RESERVED_OBJECT_TYPES.stream()
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "))
            );
        }
    }

    List<ChangeProcessor> changeProcessors()
    {
        return changeProcessors;
    }

    /// Order the available set of change processors in accordance to the requirements of the
    /// orchestrator.
    ///
    /// This algorithm creates a queue of all available models, initially in undefined order and
    /// then processes the queue until it is empty. If the item at the front of the queue cannot be
    /// placed in the ordered list yet, because it has some unsatisfied dependency, it is placed
    /// back at the end of the queue.
    ///
    /// The worst-case scenario is that all items except the last item in the queue need to be
    /// placed back in the queue for every iteration. That means the running time of this O(n^2).
    /// The maximum number of iterations is (n * (n + 1) / 2). If more is needed, then there is a
    /// dependency that can never be satisfied. That is a programming error.
    private List<ChangeProcessor> orderChangeProcessors(
        Set<ChangeProcessor> changeProcessors,
        Set<Class<?>> customObjectTypes)
    {
        var availableObjectTypes = new HashSet<>(RESERVED_OBJECT_TYPES);
        availableObjectTypes.addAll(customObjectTypes);
        var queue = new ArrayDeque<>(changeProcessors);
        var size = changeProcessors.size();
        var maxIterations = (size * (size + 1)) / 2;
        int iteration = 0;
        var result = new ArrayList<ChangeProcessor>(size);
        while (!queue.isEmpty())
        {
            iteration++;
            if (iteration > maxIterations)
            {
                throw new IllegalStateException("Dependency cycle or unsatisfied consumer.");
            }
            var processor = queue.pollFirst();
            if (!availableObjectTypes.containsAll(processor.consumedObjectTypes()))
            {
                queue.addLast(processor);
                continue;
            }
            if (producesAnyOf(queue, processor.consumedObjectTypes()))
            {
                queue.addLast(processor);
                continue;
            }
            result.add(processor);
            availableObjectTypes.addAll(processor.producedObjectTypes());
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("{} change processors will be refreshed in this order: {}", result.size(),
                result.stream().map(processor -> processor.getClass().getSimpleName())
                    .collect(Collectors.joining(", "))
            );
        }
        return copyOf(result);
    }

    /// Checks if any of the processors in the collection produce any of the given object types.
    private boolean producesAnyOf(
        Collection<ChangeProcessor> changeProcessors, Set<Class<?>> objectTypes)
    {
        return changeProcessors.stream().anyMatch(processor ->
            objectTypes.stream().anyMatch(objectType ->
                processor.producedObjectTypes().contains(objectType)));
    }

    public Changelog runFor(Change<?> change)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Executing {} change processors.", changeProcessors.size());
        }
        var changelog = changelogFor(change);
        for (ChangeProcessor processor : changeProcessors)
        {
            try
            {
                var filteredChangelog = changelog.changelogFor(processor.consumedObjectTypes());
                if (filteredChangelog.isEmpty())
                {
                    LOGGER.debug("No relevant changes for processor '{}' available. Skipping.",
                        processor.getClass().getSimpleName()
                    );
                    continue;
                }
                var newChangelog = processor.run(filteredChangelog);
                verifyChanges(processor, newChangelog);
                changelog = changelog.append(newChangelog);
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Executed change processor '{}'.",
                        processor.getClass().getSimpleName()
                    );
                }
            }
            catch (RuntimeException e)
            {
                LOGGER.error("Caught runtime exception while executing change processor '{}'.",
                    processor.getClass().getSimpleName(), e
                );
            }
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Change processor execution resulted in {} changes.",
                changelog.changes().count()
            );
        }
        return changelog;
    }

    private static void verifyChanges(ChangeProcessor processor, Changelog newChangelog)
    {
        if (newChangelog.changes()
            .anyMatch(c -> !processor.producedObjectTypes().contains(c.objectType())))
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                    "Change processor '{}' produced changes of a type that it doesn't claim to " +
                    "produce. Produced types: {}. Allowed types: {}.",
                    processor.getClass().getSimpleName(),
                    newChangelog.changes()
                        .map(Change::objectType)
                        .map(Class::getSimpleName)
                        .collect(toSet()),
                    processor.producedObjectTypes().stream()
                        .map(Class::getSimpleName)
                        .collect(toSet())
                );
            }
            throw new IllegalStateException(
                "Change processor " + processor.getClass().getSimpleName() +
                " is producing changes of a type that it doesn't claim to produce.");
        }
    }
}

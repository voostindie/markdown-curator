package nl.ulso.curator.main;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;
import nl.ulso.curator.statistics.Statistics;
import nl.ulso.curator.vault.*;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.toSet;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
final class DefaultChangeProcessorOrchestrator
    implements ChangeProcessorOrchestrator
{
    private static final Set<Class<?>> RESERVED_PAYLOAD_TYPES =
        Set.of(Vault.class, Folder.class, Document.class);
    private static final List<Class<?>> RESERVED_CHANGE_PROCESSORS_CLASSES =
        List.of(VaultReloader.class, VaultInitializer.class);

    private static final Logger LOGGER = getLogger(DefaultChangeProcessorOrchestrator.class);

    private final List<ChangeProcessor> changeProcessors;
    private final Statistics statistics;

    @Inject
    DefaultChangeProcessorOrchestrator(Set<ChangeProcessor> changeProcessors, Statistics statistics)
    {
        verifyPayloadTypeConsumers(changeProcessors);
        verifyReservedPayloadTypeProducers(changeProcessors);
        this.changeProcessors = orderChangeProcessors(changeProcessors);
        this.statistics = statistics;
    }

    private void verifyPayloadTypeConsumers(Set<ChangeProcessor> processors)
    {
        var invalidConsumers = processors.stream()
            .filter(processor -> processor.consumedPayloadTypes().isEmpty())
            .toList();
        if (!invalidConsumers.isEmpty())
        {
            throw new IllegalArgumentException(
                "Change processors must consume at least one payload type: " +
                invalidConsumers.stream()
                    .map(ChangeProcessor::getClass)
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "))
            );
        }
    }

    /// Verify that none of the provided change processors produce payload types that are reserved
    /// by the core system.
    private void verifyReservedPayloadTypeProducers(Set<ChangeProcessor> changeProcessors)
    {
        var processorsToVerify = changeProcessors.stream()
            .filter(processor -> !RESERVED_CHANGE_PROCESSORS_CLASSES.contains(processor.getClass()))
            .collect(toSet());
        if (producesAnyOf(processorsToVerify, RESERVED_PAYLOAD_TYPES))
        {
            throw new IllegalArgumentException(
                "Change processors may not produce any of the reserved payload types: " +
                RESERVED_PAYLOAD_TYPES.stream()
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
    /// This algorithm creates a queue of all available models, initially sorted on class name for
    /// consistency, and then processes the queue until it is empty. If the item at the front of the
    /// queue cannot be placed in the ordered list yet, because it has some unsatisfied dependency,
    /// it is placed back at the end of the queue for later processing.
    ///
    /// The worst-case scenario is that all items except the last item in the queue need to be
    /// placed back in the queue for every iteration. That means the running time of this O(n^2).
    /// The maximum number of iterations is (n * (n + 1) / 2). If more is needed, then there is a
    /// dependency that can never be satisfied. That is a programming error.
    ///
    /// The list of [#RESERVED_CHANGE_PROCESSORS_CLASSES] is special: instances of these classes are
    /// not ordered dynamically, but instead placed in front of the list after it has been ordered,
    /// in the order they have been declared in.
    private List<ChangeProcessor> orderChangeProcessors(Set<ChangeProcessor> changeProcessors)
    {
        var availablePayloadTypes = new HashSet<>(RESERVED_PAYLOAD_TYPES);
        var queue = new ArrayDeque<>(changeProcessors.stream()
            .filter(processor -> !RESERVED_CHANGE_PROCESSORS_CLASSES.contains(processor.getClass()))
            .sorted(comparing(c -> c.getClass().getSimpleName()))
            .toList());
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
            if (!availablePayloadTypes.containsAll(processor.consumedPayloadTypes()))
            {
                queue.addLast(processor);
                continue;
            }
            if (producesAnyOf(queue, processor.consumedPayloadTypes()))
            {
                queue.addLast(processor);
                continue;
            }
            result.add(processor);
            availablePayloadTypes.addAll(processor.producedPayloadTypes());
        }
        RESERVED_CHANGE_PROCESSORS_CLASSES.reversed().stream()
            .map(reservedClass -> changeProcessors.stream()
                .filter(processor -> processor.getClass().equals(reservedClass))
                .findAny())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(result::addFirst);
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("{} change processors will be refreshed in this order: {}", result.size(),
                result.stream().map(ChangeProcessor::name).collect(Collectors.joining(", "))
            );
        }
        return copyOf(result);
    }

    /// Checks if any of the processors in the collection produce any of the given payload types.
    private boolean producesAnyOf(
        Collection<ChangeProcessor> changeProcessors, Set<Class<?>> payloadTypes)
    {
        return changeProcessors.stream().anyMatch(processor ->
            payloadTypes.stream().anyMatch(payloadType ->
                processor.producedPayloadTypes().contains(payloadType)));
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
                var filteredChangelog = changelog.changelogFor(processor.consumedPayloadTypes());
                if (filteredChangelog.isEmpty())
                {
                    LOGGER.debug("Skipping change processor {}. No relevant changes available.",
                        processor.name()
                    );
                    continue;
                }
                LOGGER.debug("Running change processor {}.", processor.name());
                var newChangelog = processor.apply(filteredChangelog);
                verifyChanges(processor, newChangelog);
                changelog = changelog.append(newChangelog);
                LOGGER.trace("Executed change processor {}.", processor.name());
            }
            catch (RuntimeException e)
            {
                LOGGER.error("Caught runtime exception while executing change processor {}.",
                    processor.getClass().getSimpleName(), e
                );
            }
        }
        LOGGER.debug("Executing all change processors resulted in {} change(s).",
            changelog.changes().count()
        );
        var level = changelog.changesFor(Vault.class).findAny().
            map(_ -> Level.INFO)
            .orElse(Level.TRACE);
        statistics.logTo(LOGGER, level);
        return changelog;
    }

    private static void verifyChanges(ChangeProcessor processor, Changelog newChangelog)
    {
        if (newChangelog.changes()
            .anyMatch(c -> !processor.producedPayloadTypes().contains(c.payloadType())))
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                    "Change processor {} produced changes of a type that it doesn't claim to " +
                    "produce. Produced types: {}. Allowed types: {}.",
                    processor,
                    newChangelog.changes()
                        .map(Change::payloadType)
                        .map(Class::getSimpleName)
                        .collect(toSet()),
                    processor.producedPayloadTypes().stream()
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

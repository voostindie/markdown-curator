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
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSequencedSet;
import static java.util.Comparator.comparing;
import static java.util.List.copyOf;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.change.Changelog.emptyChangelog;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
final class DefaultChangeProcessorOrchestrator
    implements ChangeProcessorOrchestrator
{
    private static final Set<Class<?>> RESERVED_PAYLOAD_TYPES =
        Set.of(Reset.class, Vault.class, Folder.class, Document.class);
    private static final List<Class<?>> RESERVED_CHANGE_PROCESSORS_CLASSES =
        List.of(VaultReloader.class, ExtensionLoader.class, VaultInitializer.class);

    private static final Logger LOGGER = getLogger(DefaultChangeProcessorOrchestrator.class);

    private final List<ChangeProcessor> reservedProcessors;
    private final SequencedSet<ChangeProcessor> staticProcessors;
    private final Statistics statistics;
    private final ExtensionRegistry extensionRegistry;
    private volatile List<ChangeProcessor> orderedProcessors;

    @Inject
    DefaultChangeProcessorOrchestrator(
        Set<ChangeProcessor> changeProcessors,
        Statistics statistics,
        ExtensionRegistry extensionRegistry)
    {
        verifyPayloadTypeConsumers(changeProcessors);
        verifyReservedPayloadTypeProducers(changeProcessors);
        this.reservedProcessors = RESERVED_CHANGE_PROCESSORS_CLASSES.stream()
            .flatMap(reservedClass -> changeProcessors.stream()
                .filter(processor -> processor.getClass().equals(reservedClass)))
            .toList();
        var nonReservedProcessors = new LinkedHashSet<ChangeProcessor>();
        changeProcessors.stream()
            .filter(processor -> !isReserved(processor))
            .forEach(nonReservedProcessors::add);
        this.staticProcessors = unmodifiableSequencedSet(nonReservedProcessors);
        this.orderedProcessors = orderChangeProcessors(staticProcessors);
        this.statistics = statistics;
        this.extensionRegistry = extensionRegistry;
    }

    private static boolean isReserved(ChangeProcessor processor)
    {
        return RESERVED_CHANGE_PROCESSORS_CLASSES.contains(processor.getClass());
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
            .filter(processor -> !isReserved(processor))
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

    /// @return all change processors in the order they are executed.
    List<ChangeProcessor> changeProcessors()
    {
        var processors = orderedProcessors;
        var result = new ArrayList<ChangeProcessor>(reservedProcessors.size() + processors.size());
        result.addAll(reservedProcessors);
        result.addAll(processors);
        return unmodifiableList(result);
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
    /// not ordered dynamically, but instead always run first, in the order they have been declared
    /// in. They are not part of the set passed to this method.
    private List<ChangeProcessor> orderChangeProcessors(Set<ChangeProcessor> changeProcessors)
    {
        var availablePayloadTypes = new HashSet<>(RESERVED_PAYLOAD_TYPES);
        var queue = new ArrayDeque<>(changeProcessors.stream()
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
                throw new IllegalStateException(
                    "Dependency cycle or unsatisfied consumer(s). The following change " +
                    "processor(s) cannot be put in order: " +
                    queue.stream()
                        .map(ChangeProcessor::name)
                        .collect(joining(", "))
                );
            }
            var processor = queue.pollFirst();
            var neededPayloadTypes = HashSet.<Class<?>>newHashSet(
                processor.consumedPayloadTypes().size() + processor.requiredPayloadTypes().size());
            neededPayloadTypes.addAll(processor.consumedPayloadTypes());
            neededPayloadTypes.addAll(processor.requiredPayloadTypes());
            if (!availablePayloadTypes.containsAll(neededPayloadTypes) ||
                producesAnyOf(queue, neededPayloadTypes))
            {
                queue.addLast(processor);
            }
            else
            {
                result.add(processor);
                availablePayloadTypes.addAll(processor.producedPayloadTypes());
            }
        }
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("{} change processors will be refreshed in this order: {}",
                reservedProcessors.size() + result.size(),
                Stream.concat(reservedProcessors.stream(), result.stream())
                    .map(ChangeProcessor::name)
                    .collect(Collectors.joining(", "))
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

    public Changelog runFor(List<Change<?>> changes)
    {
        var changelog = changelogFor(changes);
        resetIfNeeded(changelog);
        changelog = runProcessors(reservedProcessors, changelog);
        if (changelog.changes().anyMatch(isPayloadType(Reset.class)))
        {
            activateStagedExtensions();
        }
        var processors = orderedProcessors;
        changelog = runProcessors(processors, changelog);
        LOGGER.info(
            "Executed {} change processors on {} change(s).",
            reservedProcessors.size() + processors.size(),
            changes.size()
        );
        statistics.logTo(
            LOGGER,
            changelog.changes().anyMatch(isPayloadType(Reset.class)) ? Level.INFO : Level.TRACE
        );
        LOGGER.info("Produced changelog with {} change(s).", changelog.size());
        return changelog;
    }

    private Changelog runProcessors(List<ChangeProcessor> processors, Changelog changelog)
    {
        for (ChangeProcessor processor : processors)
        {
            var processorName = processor.name();
            var filteredChangelog = changelog.changelogFor(processor.consumedPayloadTypes());
            if (filteredChangelog.isEmpty())
            {
                LOGGER.debug(
                    "Skipping change processor {}. No relevant changes available.",
                    processorName
                );
                continue;
            }
            LOGGER.debug("Running change processor {}.", processorName);
            var newChangelog = safelyApplyChangelogTo(processor, filteredChangelog);
            verifyChanges(processor, newChangelog);
            if (resetIfNeeded(newChangelog))
            {
                // The new changelog contains a Reset. All changes before it are no longer
                // relevant and can be ignored going forward.
                changelog = newChangelog;
            }
            else
            {
                // Append the new changelog to the existing one for the next processor.
                changelog = changelog.append(newChangelog);
            }
            LOGGER.trace("Executed change processor {}.", processorName);
        }
        return changelog;
    }

    /// Activates the extensions staged by the [ExtensionLoader], if any.
    ///
    /// This happens only during a [Reset], after the reserved processors have run and before all
    /// other processors run. At that moment all processors have been reset and the vault
    /// initialization changes are in the changelog, so the change processors in the new extensions
    /// will receive the complete state of the vault, as will all existing processors.
    ///
    /// If the new set of change processors is invalid - for example because an extension consumes
    /// a payload type nobody produces, or because a query name is already taken - then the new
    /// configuration is rejected as a whole and the current one stays active.
    private void activateStagedExtensions()
    {
        extensionRegistry.takeStaged().ifPresent(extensions -> {
            try
            {
                // Ordered sets and maps make the ordering of change processors predictable.
                var processors = new LinkedHashSet<>(staticProcessors);
                new TreeMap<>(extensions).values().forEach(extension ->
                    processors.addAll(extension.changeProcessors()));
                verifyPayloadTypeConsumers(processors);
                verifyReservedPayloadTypeProducers(processors);
                var ordered = orderChangeProcessors(processors);
                extensionRegistry.activate(extensions);
                orderedProcessors = ordered;
                LOGGER.info("Activated {} extension(s) with {} change processor(s) in total.",
                    extensions.size(), reservedProcessors.size() + ordered.size()
                );
            }
            catch (RuntimeException e)
            {
                LOGGER.error("Rejecting the new extensions. Keeping the current extensions.", e);
                extensionRegistry.reject(e.getMessage());
            }
        });
    }

    private boolean resetIfNeeded(Changelog changelog)
    {
        if (changelog.changes().noneMatch(isPayloadType(Reset.class)))
        {
            return false;
        }
        LOGGER.info("Resetting all change processors.");
        reservedProcessors.forEach(DefaultChangeProcessorOrchestrator::safelyReset);
        orderedProcessors.forEach(DefaultChangeProcessorOrchestrator::safelyReset);
        return true;
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

    private Changelog safelyApplyChangelogTo(ChangeProcessor processor, Changelog changelog)
    {
        try
        {
            return processor.apply(changelog);
        }
        catch (RuntimeException e)
        {
            logProcessorError(processor, e);
        }
        return emptyChangelog();
    }

    private static void safelyReset(ChangeProcessor processor)
    {
        try
        {
            processor.reset();
        }
        catch (RuntimeException e)
        {
            logProcessorError(processor, e);
        }
    }

    private static void logProcessorError(ChangeProcessor processor, RuntimeException e)
    {
        LOGGER.error(
            "Caught runtime exception while executing change processor {}. Check the code!",
            processor.getClass().getSimpleName(), e
        );
    }
}

package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.addon.journal.*;
import nl.ulso.curator.addon.project.*;
import nl.ulso.curator.change.ChangeHandler;

import java.util.*;

import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

/// Base class for attribute value producers that trigger on [ProjectMarker]s to determine the
/// attribute value.
///
/// This implementation implements the [#resolveAttributeValue(Project, Daily)] method by looking
/// for the known project marker links in the daily journal entry. If found, the actual attribute
/// value is resolved from that line in the journal by calling
/// [#resolveAttributeValue(ProjectMarker, String, String)], which subclasses have to implement.
///
/// This implementation subscribes to [ProjectMarker] events. If one comes in, it forces a complete
/// reload. This is done because implementing the correct event-based logic is complex, while it
/// hardly ever happens.
///
/// This implementation supports multiple project markers for the same attribute definition. In
/// practice there's just one (or zero).
abstract class ProjectMarkerBasedAttributeValueProducer<P extends ProjectMarker>
    extends ProjectJournalAttributeValueProducer
{
    private final ProjectMarkerRepository<P> projectMarkerRepository;

    ProjectMarkerBasedAttributeValueProducer(
        ProjectRepository projectRepository,
        Journal journal,
        ProjectAttributeDefinition attributeDefinition,
        ProjectMarkerRepository<P> projectMarkerRepository)
    {
        super(projectRepository, journal, attributeDefinition);
        this.projectMarkerRepository = projectMarkerRepository;
    }

    @Override
    public final Set<Class<?>> consumedPayloadTypes()
    {
        var payloadTypes = new HashSet<>(super.consumedPayloadTypes());
        payloadTypes.add(projectMarkerType());
        return payloadTypes;
    }

    @Override
    protected final List<? extends ChangeHandler> createChangeHandlers()
    {
        var handlers = new ArrayList<ChangeHandler>();
        handlers.add(newChangeHandler(
            isPayloadType(projectMarkerType()),
            (_, collector) -> reload(collector)
        ));
        handlers.addAll(super.createChangeHandlers());
        return handlers;
    }

    /// Search for matching project marker links for the project in the daily journal entry. For
    /// each marker (typically just one), we go through the daily from bottom to top, under the
    /// assumption that it's chronological.
    ///
    /// In practice there's just one marker in use, and a single project marker link in the same
    /// daily, because why have multiple? But it works!
    ///
    /// In case there are multiple markers, then they are processed in undefined order. This might
    /// lead to inconsistent behavior between different application runs, but only if multiple
    /// markers are used in the same daily. This is considered too rare to care about.
    @Override
    final Optional<Object> resolveAttributeValue(Project project, Daily daily)
    {
        var entries = daily.markedLinesFor(
            project.name(),
            projectMarkerRepository.allMarkers(),
            false
        );
        for (Map.Entry<String, List<MarkedLine>> entry : entries.entrySet())
        {
            var marker = projectMarkerRepository.markerNamed(entry.getKey());
            for (var markedLine : entry.getValue().reversed())
            {
                var value = findMarkerLink(marker, markedLine.line())
                    .flatMap(link -> resolveAttributeValue(marker, link, markedLine.line()));
                if (value.isPresent())
                {
                    return value;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> findMarkerLink(P marker, String line)
    {
        for (var link : marker.markdownLinks().keySet())
        {
            if (line.contains(link))
            {
                return Optional.of(link);
            }
        }
        return Optional.empty();
    }

    protected abstract Optional<Object> resolveAttributeValue(P marker, String link, String line);

    protected abstract Class<P> projectMarkerType();
}

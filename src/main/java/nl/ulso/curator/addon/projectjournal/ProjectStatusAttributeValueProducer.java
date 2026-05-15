package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.addon.project.ProjectAttributeDefinition;
import nl.ulso.curator.addon.project.ProjectRepository;

import java.util.Map;
import java.util.Optional;

import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.STATUS;

/// Resolves the status of projects from the most recent daily journal entry they were mentioned
/// in.
@Singleton
final class ProjectStatusAttributeValueProducer
    extends ProjectMarkerBasedAttributeValueProducer<ProjectStatusMarker>
{
    @Inject
    ProjectStatusAttributeValueProducer(
        ProjectRepository projectRepository,
        Journal journal,
        Map<String, ProjectAttributeDefinition> attributeDefinitions,
        ProjectStatusMarkerRepository projectMarkerRepository)
    {
        super(
            projectRepository,
            journal,
            attributeDefinitions.get(STATUS),
            projectMarkerRepository
        );
    }

    @Override
    protected Class<ProjectStatusMarker> projectMarkerType()
    {
        return ProjectStatusMarker.class;
    }

    @Override
    protected Optional<Object> resolveAttributeValue(
        ProjectStatusMarker marker,
        String link,
        String line)
    {
        return Optional.of(marker.resolveStatusFrom(link));
    }
}

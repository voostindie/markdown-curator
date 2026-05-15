package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.addon.project.ProjectAttributeDefinition;
import nl.ulso.curator.addon.project.ProjectRepository;
import nl.ulso.curator.vault.Vault;

import java.util.Map;
import java.util.Optional;

import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.LEAD;

/// Resolves the status of projects from the most recent daily journal entry they were mentioned
/// in.
@Singleton
final class ProjectLeadAttributeValueProducer
    extends ProjectMarkerBasedAttributeValueProducer<ProjectLeadMarker>
{
    private final Vault vault;

    @Inject
    ProjectLeadAttributeValueProducer(
        ProjectRepository projectRepository,
        Journal journal,
        Map<String, ProjectAttributeDefinition> attributeDefinitions,
        ProjectLeadMarkerRepository projectMarkerRepository,
        Vault vault)
    {
        super(
            projectRepository,
            journal,
            attributeDefinitions.get(LEAD),
            projectMarkerRepository
        );
        this.vault = vault;

    }

    @Override
    protected Class<ProjectLeadMarker> projectMarkerType()
    {
        return ProjectLeadMarker.class;
    }

    @Override
    protected Optional<Object> resolveAttributeValue(
        ProjectLeadMarker marker,
        String link,
        String line)
    {
        return marker.resolveLeadFrom(line, vault);
    }
}

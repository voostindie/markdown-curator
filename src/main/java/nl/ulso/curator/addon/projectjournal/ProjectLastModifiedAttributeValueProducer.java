package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.journal.Daily;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.addon.project.*;

import java.util.Map;
import java.util.Optional;

import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.LAST_MODIFIED;

/// Resolves the last modification date of projects from the most recent daily journal entry they
/// were mentioned in.
@Singleton
final class ProjectLastModifiedAttributeValueProducer
    extends ProjectJournalAttributeValueProducer
{
    @Inject
    ProjectLastModifiedAttributeValueProducer(
        ProjectRepository projectRepository,
        Journal journal,
        Map<String, ProjectAttributeDefinition> attributeDefinitions)
    {
        super(projectRepository, journal, attributeDefinitions.get(LAST_MODIFIED));
    }

    @Override
    Optional<Object> resolveAttributeValue(Project project, Daily daily)
    {
        return Optional.of(daily.date());
    }
}

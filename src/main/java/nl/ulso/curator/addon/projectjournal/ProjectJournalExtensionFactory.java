package nl.ulso.curator.addon.projectjournal;

import jakarta.inject.Inject;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.addon.project.ProjectAttributeDefinition;
import nl.ulso.curator.addon.project.ProjectRepository;
import nl.ulso.curator.extension.*;
import nl.ulso.dictionary.Dictionary;

import java.util.Set;

import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.byFrontMatterProperty;

/// Creates the project journal extension, which has no settings. It requires both the project and
/// the journal extensions.
final class ProjectJournalExtensionFactory
    implements ExtensionFactory
{
    private final ProjectJournalExtension.Factory factory;

    @Inject
    ProjectJournalExtensionFactory(ProjectJournalExtension.Factory factory)
    {
        this.factory = factory;
    }

    @Override
    public String description()
    {
        return "Resolves the lead, status and last modification date of projects from the journal.";
    }

    @Override
    public Set<Class<?>> requires()
    {
        return Set.of(ProjectRepository.class, Journal.class);
    }

    @Override
    public Extension create(Dictionary configuration, Services services)
    {
        return factory.create(
            services.get(ProjectRepository.class),
            services.get(Journal.class),
            byFrontMatterProperty(services.all(ProjectAttributeDefinition.class))
        );
    }
}

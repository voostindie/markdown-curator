package nl.ulso.curator.addon.projectjournal;

import dagger.Component;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.journal.JournalExtensionModule;
import nl.ulso.curator.addon.project.*;
import nl.ulso.curator.main.ChangeProcessorOrchestrator;
import nl.ulso.curator.main.ChangeProcessorTestModule;
import nl.ulso.curator.main.ExtensionTestHarness;
import nl.ulso.curator.query.DaggerQueryTestModule;
import nl.ulso.curator.vault.VaultStub;

/// Loads the project, journal and project journal extensions, with their default settings.
@Singleton
@Component(modules = {
    ChangeProcessorTestModule.class,
    DaggerQueryTestModule.class,
    ProjectExtensionModule.class,
    JournalExtensionModule.class,
    ProjectJournalExtensionModule.class}
)
interface ProjectJournalTestContext
{
    VaultStub vaultStub();

    ChangeProcessorOrchestrator changeProcessorOrchestrator();

    ExtensionTestHarness harness();

    /// Loads all extensions and processes the complete vault.
    default void start()
    {
        harness().enable("project").enable("journal").enable("projectjournal").start();
    }

    default ProjectRepository projectRepository()
    {
        return harness().find(ProjectRepository.class);
    }

    default ProjectAttributeRepository projectAttributeRepository()
    {
        return harness().find(ProjectAttributeRepository.class);
    }

    default ProjectLastModifiedAttributeValueProducer projectAttributeLastModifiedValueProducer()
    {
        return harness().find(ProjectLastModifiedAttributeValueProducer.class);
    }
}

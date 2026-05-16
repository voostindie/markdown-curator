package nl.ulso.curator.addon.projectjournal;

import dagger.Component;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.project.ProjectAttributeRepository;
import nl.ulso.curator.addon.project.ProjectRepository;
import nl.ulso.curator.main.ChangeProcessorOrchestrator;
import nl.ulso.curator.main.ChangeProcessorTestModule;
import nl.ulso.curator.vault.VaultStub;

@Singleton
@Component(modules = {
    ChangeProcessorTestModule.class,
    ProjectJournalModule.class,
    ProjectJournalTestModule.class}
)
interface ProjectJournalTestContext
{
    VaultStub vaultStub();

    ChangeProcessorOrchestrator changeProcessorOrchestrator();

    ProjectRepository projectRepository();

    ProjectAttributeRepository projectAttributeRepository();

    ProjectLastModifiedAttributeValueProducer projectAttributeLastModifiedValueProducer();
}

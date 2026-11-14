package nl.ulso.curator.addon.omnifocus;

import dagger.Component;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.project.*;
import nl.ulso.curator.main.ChangeProcessorOrchestrator;
import nl.ulso.curator.main.ChangeProcessorTestModule;
import nl.ulso.curator.main.ExtensionTestHarness;
import nl.ulso.curator.query.DaggerQueryTestModule;
import nl.ulso.curator.vault.VaultStub;

import java.util.Map;

/// Loads the project and OmniFocus extensions; OmniFocus projects are fetched from the folder
/// "Area". The OmniFocus database is this project's README, which is guaranteed to exist.
@Singleton
@Component(modules = {
    ChangeProcessorTestModule.class,
    DaggerQueryTestModule.class,
    ProjectExtensionModule.class,
    OmniFocusTestModule.class}
)
interface OmniFocusTestContext
{
    VaultStub vaultStub();

    JavaScriptForAutomationStub javascriptForAutomationStub();

    ChangeProcessorOrchestrator changeProcessorOrchestrator();

    ExtensionTestHarness harness();

    /// Loads all extensions and processes the complete vault.
    default void start()
    {
        harness().enable("project").enable("omnifocus", Map.of("folder", "Area", "database", "README.md")).start();
    }

    default ProjectRepository projectRepository()
    {
        return harness().find(ProjectRepository.class);
    }

    default ProjectAttributeRepository projectAttributeRepository()
    {
        return harness().find(ProjectAttributeRepository.class);
    }

    default DefaultOmniFocusRepository omniFocusRepository()
    {
        return harness().find(DefaultOmniFocusRepository.class);
    }

    default OmniFocusProjectAttributeValueProducer omniFocusProjectAttributeValueProducer()
    {
        return harness().find(OmniFocusProjectAttributeValueProducer.class);
    }

    default OmniFocusQuery omniFocusQuery()
    {
        return harness().find(OmniFocusQuery.class);
    }
}

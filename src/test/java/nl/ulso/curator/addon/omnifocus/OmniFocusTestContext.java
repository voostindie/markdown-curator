package nl.ulso.curator.addon.omnifocus;

import dagger.Component;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.project.*;
import nl.ulso.curator.main.ChangeProcessorOrchestrator;
import nl.ulso.curator.main.ChangeProcessorTestModule;
import nl.ulso.curator.query.DaggerQueryTestModule;
import nl.ulso.curator.vault.VaultStub;

@Singleton
@Component(modules = {
    ChangeProcessorTestModule.class,
    DaggerQueryTestModule.class,
    ProjectModule.class,
    OmniFocusTestModule.class}
)
interface OmniFocusTestContext
{
    VaultStub vaultStub();

    OmniFocusDatabaseStub omniFocusDatabaseStub();

    ExternalChangeHandlerStub externalChangeHandlerStub();

    JavaScriptForAutomationStub javascriptForAutomationStub();

    ChangeProcessorOrchestrator changeProcessorOrchestrator();

    ProjectRepository projectRepository();

    ProjectAttributeRepository projectAttributeRepository();

    DefaultOmniFocusRepository omniFocusRepository();

    OmniFocusProjectAttributeValueProducer omniFocusProjectAttributeValueProducer();

    OmniFocusQuery omniFocusQuery();
}

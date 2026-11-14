package nl.ulso.curator.addon.backlink;

import dagger.Component;
import jakarta.inject.Singleton;
import nl.ulso.curator.main.ChangeProcessorTestModule;
import nl.ulso.curator.main.ExtensionTestHarness;
import nl.ulso.curator.query.DaggerQueryTestModule;
import nl.ulso.curator.vault.VaultStub;

/// Loads the backlinks extension on top of a vault stub.
@Singleton
@Component(modules = {
    ChangeProcessorTestModule.class,
    DaggerQueryTestModule.class,
    BacklinkExtensionModule.class}
)
interface BacklinkTestContext
{
    VaultStub vaultStub();

    ExtensionTestHarness harness();

    /// Loads the extension and processes the complete vault.
    default void start()
    {
        harness().enable("backlinks").start();
    }

    default BacklinkRepository backlinkRepository()
    {
        return harness().find(BacklinkRepository.class);
    }

    default BacklinkQueryReferenceRepository referenceRepository()
    {
        return harness().find(BacklinkQueryReferenceRepository.class);
    }

    default BacklinkQuery query()
    {
        return harness().find(BacklinkQuery.class);
    }
}

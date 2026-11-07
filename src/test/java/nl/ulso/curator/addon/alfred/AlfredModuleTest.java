package nl.ulso.curator.addon.alfred;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import nl.ulso.curator.CuratorFactory;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.main.ExtensionTestHarness;
import nl.ulso.curator.addon.journal.JournalExtensionModule;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/// Loads the extension(s) into a real curator, on top of the music vault.
@ExtendWith(SoftAssertionsExtension.class)
class AlfredModuleTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void loadExtensionsIntoCurator()
    {
        var factory = DaggerAlfredModuleTest_FactoryStub.create();
        softly.assertThat(factory.createCurator()).isNotNull();
        var harness = factory.harness();
        harness.enable("journal").enable("alfred", Map.of("projects-path", "Projects")).start();
        softly.assertThat(harness.failures()).isEmpty();
        softly.assertThat(harness.activeExtensions()).containsExactly("alfred", "journal");
        softly.assertThat(harness.find(AlfredQuery.class)).isNotNull();
        softly.assertThat(harness.query("links")).isInstanceOf(ExternalLinksQuery.class);
    }

    @Singleton
    @Component(modules = ModuleStub.class)
    public interface FactoryStub
        extends CuratorFactory
    {
        @Override
        default String name()
        {
            return "Stub";
        }

        ExtensionTestHarness harness();
    }

    @Module(includes = {
        CuratorModule.class,
        JournalExtensionModule.class,
        AlfredExtensionModule.class})
    static class ModuleStub
    {
        @Provides
        public Path vaultPath()
        {
            return Paths.get("").toAbsolutePath().resolve("src/test/resources/music");
        }
    }
}

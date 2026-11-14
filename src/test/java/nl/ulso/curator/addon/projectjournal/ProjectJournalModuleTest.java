package nl.ulso.curator.addon.projectjournal;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import nl.ulso.curator.CuratorFactory;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.main.ExtensionTestHarness;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.addon.project.ProjectRepository;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;

/// Loads the extension(s) into a real curator, on top of the music vault.
@ExtendWith(SoftAssertionsExtension.class)
class ProjectJournalModuleTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void loadExtensionsIntoCurator()
    {
        var factory = DaggerProjectJournalModuleTest_FactoryStub.create();
        softly.assertThat(factory.createCurator()).isNotNull();
        var harness = factory.harness();
        harness.enable("project").enable("journal").enable("projectjournal").start();
        softly.assertThat(harness.failures()).isEmpty();
        softly.assertThat(harness.activeExtensions())
            .containsExactly("journal", "project", "projectjournal");
        softly.assertThat(harness.find(Journal.class)).isNotNull();
        softly.assertThat(harness.find(ProjectRepository.class)).isNotNull();
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

    @Module(includes = CuratorModule.class)
    static class ModuleStub
    {
        @Provides
        public Path vaultPath()
        {
            return Paths.get("").toAbsolutePath().resolve("src/test/resources/music");
        }
    }
}

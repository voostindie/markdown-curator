package nl.ulso.curator.addon.projectjournal;

import dagger.*;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.ulso.curator.CuratorFactory;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.addon.journal.JournalSettings;
import nl.ulso.curator.addon.project.ProjectRepository;
import nl.ulso.curator.addon.project.ProjectSettings;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;

@ExtendWith(SoftAssertionsExtension.class)
class ProjectJournalModuleTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;


    @Test
    void createModuleWithInjector()
    {
        var factory = DaggerProjectJournalModuleTest_FactoryStub.create();
        var curator = factory.createCurator();
        softly.assertThat(curator).isNotNull();
        softly.assertThat(factory.getJournal()).isNotNull();
        softly.assertThat(factory.getProjectRepository()).isNotNull();
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

        Journal getJournal();

        ProjectRepository getProjectRepository();
    }

    @Module(includes = {
            CuratorModule.class,
            ProjectJournalModule.class})
    static class ModuleStub
    {
        @Provides
        public Path vaultPath()
        {
            return Paths.get("").toAbsolutePath().resolve("src/test/resources/music");
        }

        @Provides
        public JournalSettings provideJournalSettings()
        {
            return new JournalSettings("journal", "markers", "Activities", "Projects");
        }

        @Provides
        public ProjectSettings provideProjectSettings()
        {
            return new ProjectSettings("Projects");
        }
    }
}

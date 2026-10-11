package nl.ulso.curator.addon.alfred;

import dagger.*;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.ulso.curator.CuratorFactory;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.addon.journal.JournalSettings;
import nl.ulso.curator.addon.project.ProjectSettings;
import nl.ulso.curator.query.Query;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@ExtendWith(SoftAssertionsExtension.class)
class AlfredModuleTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void createModuleWithInjector()
    {
        var factory = DaggerAlfredModuleTest_FactoryStub.create();
        var curator = factory.createCurator();
        softly.assertThat(curator).isNotNull();
        var queries = factory.queries();
        softly.assertThat(queries).anyMatch(query -> query instanceof AlfredQuery);
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

        Set<Query> queries();
    }

    @Module(includes = {
        CuratorModule.class,
        AlfredModule.class})
    static class ModuleStub
    {
        @Provides
        public Path vaultPath()
        {
            return Paths.get("").toAbsolutePath().resolve("src/test/resources/music");
        }

        @Provides
        JournalSettings provideJournalSettings()
        {
            return new JournalSettings("Journal", "Markers", "Activities", "Projects");
        }

        @Provides
        ProjectSettings provideProjectSettings()
        {
            return new ProjectSettings("Projects");
        }
    }
}

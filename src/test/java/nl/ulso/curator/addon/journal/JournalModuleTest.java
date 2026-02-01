package nl.ulso.curator.addon.journal;

import dagger.*;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.CuratorFactory;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;

@ExtendWith(SoftAssertionsExtension.class)
class JournalModuleTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;


    @Test
    void createModuleWithInjector()
    {
        var factory = DaggerJournalModuleTest_FactoryStub.create();
        var curator = factory.createCurator();
        softly.assertThat(curator).isNotNull();
        var journal = factory.getJournal();
        softly.assertThat(journal).isNotNull();
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
    }

    @Module(includes = {
            CuratorModule.class,
            JournalModule.class})
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
    }
}

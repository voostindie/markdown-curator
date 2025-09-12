package nl.ulso.markdown_curator.project;

import dagger.*;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.CuratorFactory;
import nl.ulso.markdown_curator.CuratorModule;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;

@ExtendWith(SoftAssertionsExtension.class)
class ProjectModuleTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void createModuleWithInjector()
    {
        var factory = DaggerProjectModuleTest_FactoryStub.create();
        var curator = factory.createCurator();
        softly.assertThat(curator).isNotNull();
    }

    @Test
    void eachPropertyHasOneResolver()
    {
        var factory = DaggerProjectModuleTest_FactoryStub.create();
        var registry = factory.createProjectPropertyResolverRegistry();
        softly.assertThat(registry).isNotNull();
        var repository = factory.createProjectPropertyRepository();
        softly.assertThat(repository).isNotNull();
        var properties = repository.projectProperties();
        softly.assertThat(properties).hasSize(4);
        properties.forEach(
                (key, value) -> softly.assertThat(registry.resolversFor(value)).hasSize(1));
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

        ProjectPropertyResolverRegistry createProjectPropertyResolverRegistry();

        ProjectPropertyRepository createProjectPropertyRepository();
    }

    @Module(includes = {
            CuratorModule.class,
            ProjectModule.class})
    static class ModuleStub
    {
        @Provides
        public Path vaultPath()
        {
            return Paths.get("").toAbsolutePath().resolve("src/test/resources/music");
        }

        @Provides
        public ProjectSettings provideProjectSettings()
        {
            return new ProjectSettings("Projects");
        }
    }
}

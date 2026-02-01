package nl.ulso.curator.addon.project;

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
import java.util.Map;

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
        var producer = factory.frontMatterAttributeProducer();
        softly.assertThat(producer).isNotNull();
        var repository = factory.attributeRegistry();
        softly.assertThat(repository).isNotNull();
        var attributes = factory.attributeDefinitions();
        softly.assertThat(attributes).hasSize(4);
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

        FrontMatterAttributeProducer frontMatterAttributeProducer();

        Map<String, AttributeDefinition> attributeDefinitions();

        AttributeRegistry attributeRegistry();
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

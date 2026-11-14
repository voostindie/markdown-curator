package nl.ulso.curator.addon.project;

import dagger.Component;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import nl.ulso.curator.CuratorFactory;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.main.ExtensionTestHarness;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;

/// Loads the extension(s) into a real curator, on top of the music vault.
@ExtendWith(SoftAssertionsExtension.class)
class ProjectModuleTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void loadExtensionsIntoCurator()
    {
        var factory = DaggerProjectModuleTest_FactoryStub.create();
        softly.assertThat(factory.createCurator()).isNotNull();
        var harness = factory.harness();
        harness.enable("project").start();
        softly.assertThat(harness.failures()).isEmpty();
        softly.assertThat(harness.activeExtensions()).containsExactly("project");
        softly.assertThat(harness.find(FrontMatterProjectAttributeValueProducer.class)).isNotNull();
        softly.assertThat(harness.find(ProjectAttributeRepository.class).attributeDefinitions())
            .hasSize(4);
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

package nl.ulso.markdown_curator.links;

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
class LinksModuleTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;


    @Test
    void createModuleWithInjector()
    {
        var factory = DaggerLinksModuleTest_FactoryStub.create();
        var curator = factory.createCurator();
        softly.assertThat(curator).isNotNull();
        var linksModel = factory.createLinksModel();
        softly.assertThat(linksModel).isNotNull();
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

        LinksModel createLinksModel();
    }

    @Module(includes = {
            CuratorModule.class,
            LinksModule.class})
    static class ModuleStub
    {
        @Provides
        public Path vaultPath()
        {
            return Paths.get("").toAbsolutePath().resolve("src/test/resources/music");
        }
    }
}

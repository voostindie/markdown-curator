package nl.ulso.curator.addon.newday;

import dagger.*;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.ulso.curator.CuratorFactory;
import nl.ulso.curator.CuratorModule;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;

@ExtendWith(SoftAssertionsExtension.class)
class NewDayModuleTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void createModuleWithInjector()
    {
        var factory = DaggerNewDayModuleTest_FactoryStub.create();
        var curator = factory.createCurator();
        softly.assertThat(curator).isNotNull();
        var newDayAlarm = factory.newDayAlarm();
        softly.assertThat(newDayAlarm).isNotNull();
        softly.assertThat(newDayAlarm).isInstanceOf(DefaultNewDayAlarm.class);
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

        NewDayAlarm newDayAlarm();
    }

    @Module(includes = {
            CuratorModule.class,
            NewDayModule.class})
    static class ModuleStub
    {
        @Provides
        public Path vaultPath()
        {
            return Paths.get("").toAbsolutePath().resolve("src/test/resources/music");
        }
    }
}

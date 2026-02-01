package nl.ulso.curator.main;

import dagger.Module;
import dagger.*;
import nl.ulso.curator.*;
import nl.ulso.curator.changelog.ChangeProcessor;
import nl.ulso.curator.changelog.Changelog;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.*;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static nl.ulso.curator.changelog.Changelog.emptyChangelog;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class CuratorModuleTest
{
    @Test
    void createCuratorWithInjector()
    {
        var factory = DaggerCuratorModuleTest_FactoryStub.create();
        var curator = factory.createCurator();
        assertThat(curator).isNotNull();
    }

    @Test
    void iCloudIAWriterFolder()
    {
        var path = VaultPaths.iCloudIAWriterFolder("Test");
        var home = System.getProperty("user.home");
        assertThat(path).hasToString(
                home + "/Library/Mobile Documents/27N4MQEA55~pro~writer/Documents/Test");
    }

    @Test
    void iCloudObsidianFolder()
    {
        var path = VaultPaths.iCloudObsidianVault("Test");
        var home = System.getProperty("user.home");
        assertThat(path).hasToString(
                home + "/Library/Mobile Documents/iCloud~md~obsidian/Documents/Test");
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

    @Singleton
    private static class ChangeProcessorStub
            implements ChangeProcessor
    {
        @Override
        public Changelog run(Changelog changelog)
        {
            return emptyChangelog();
        }

        @Override
        public Set<Class<?>> consumedPayloadTypes()
        {
            return Set.of(Vault.class, Document.class, Folder.class);
        }
    }

    private static class QueryStub
            implements Query
    {
        @Override
        public String name()
        {
            return "stub";
        }

        @Override
        public String description()
        {
            return "No description";
        }

        @Override
        public Map<String, String> supportedConfiguration()
        {
            return emptyMap();
        }

        @Override
        public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
        {
            return false;
        }

        @Override
        public QueryResult run(QueryDefinition definition)
        {
            // Do nothing
            return () -> "";
        }
    }
}

package nl.ulso.markdown_curator;

import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.google.inject.Guice.createInjector;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class CuratorModuleTest
{
    @Test
    void createCuratorWithInjector()
    {
        assertThat(createInjector(new ModuleStub()).getInstance(Curator.class)).isNotNull();
    }

    @Test
    void iCloudIAWriterFolder()
    {
        var path = new ModuleStub().iCloudIAWriterFolder("Test");
        var home = System.getProperty("user.home");
        assertThat(path).hasToString(
                home + "/Library/Mobile Documents/27N4MQEA55~pro~writer/Documents/Test");
    }

    @Test
    void iCloudObsidianFolder()
    {
        var path = new ModuleStub().iCloudObsidianVault("Test");
        var home = System.getProperty("user.home");
        assertThat(path).hasToString(
                home + "/Library/Mobile Documents/iCloud~md~obsidian/Documents/Test");
    }

    private static class ModuleStub
            extends CuratorModule
    {
        @Override
        public String name()
        {
            return "Stub";
        }

        @Override
        public Path vaultPath()
        {
            return Paths.get("").toAbsolutePath().resolve("src/test/resources/music");
        }

        @Override
        protected void configureCurator()
        {
            super.configureCurator();
            registerDataModel(DataModelStub.class);
            registerQuery(QueryStub.class);
        }
    }

    @Singleton
    private static class DataModelStub
            implements DataModel
    {
        @Override
        public void vaultChanged(VaultChangedEvent event)
        {
            // Do nothing
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
        public QueryResult run(QueryDefinition definition)
        {
            // Do nothing
            return () -> "";
        }
    }
}

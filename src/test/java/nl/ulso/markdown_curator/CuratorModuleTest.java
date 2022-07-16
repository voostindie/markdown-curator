package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.google.inject.Guice.createInjector;
import static java.util.Collections.emptyMap;
import static nl.ulso.markdown_curator.query.QueryResult.emptyResult;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class CuratorModuleTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void createCuratorWithInjector()
    {
        assertThat(createInjector(new ModuleStub()).getInstance(Curator.class)).isNotNull();
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
            return emptyResult();
        }
    }
}

package nl.ulso.markdown_curator.query;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static nl.ulso.markdown_curator.query.QueryResult.error;
import static nl.ulso.markdown_curator.vault.QueryBlockTest.emptyQueryBlock;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class InMemoryQueryCatalogTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void emptyCatalog()
    {
        var catalog = new InMemoryQueryCatalog();
        softly.assertThat(catalog.isEmpty()).isTrue();
        var specification = catalog.query("invalid");
        softly.assertThat(specification.name()).isEqualTo("invalid");
        softly.assertThat(specification.description()).contains("Does nothing");
        softly.assertThat(specification.supportedConfiguration()).isEmpty();
        var result = specification.run(emptyQueryBlock());
        softly.assertThat(result.toMarkdown()).contains("no queries defined");
    }

    @Test
    void registerQueryOnce()
    {
        var query = new DummyQuery("q");
        var catalog = new InMemoryQueryCatalog();
        catalog.register(query);
        softly.assertThat(catalog.query("q")).isSameAs(query);
        softly.assertThat(catalog.isEmpty()).isFalse();
    }

    @Test
    void registerQueriesSameNameOverwrites()
    {
        var q1 = new DummyQuery("q");
        var q2 = new DummyQuery("q");
        var catalog = new InMemoryQueryCatalog();
        catalog.register(q1);
        catalog.register(q2);
        assertThat(catalog.query("q")).isSameAs(q2);
    }

    private static class DummyQuery
            implements Query
    {
        private final String name;

        public DummyQuery(String name)
        {
            this.name = name;
        }

        @Override
        public String name()
        {
            return name;
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
            return error("Not implemented");
        }
    }
}

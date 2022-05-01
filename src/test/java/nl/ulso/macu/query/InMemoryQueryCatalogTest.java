package nl.ulso.macu.query;

import nl.ulso.macu.vault.QueryBlock;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static nl.ulso.macu.query.QueryResult.failure;
import static nl.ulso.macu.vault.QueryBlockTest.emptyQueryBlock;
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
        var specification = catalog.query("invalid");
        softly.assertThat(specification.name()).isEqualTo("invalid");
        softly.assertThat(specification.description()).contains("does nothing");
        softly.assertThat(specification.supportedConfiguration()).isEmpty();
        var result = specification.run(emptyQueryBlock());
        softly.assertThat(result.isSuccess()).isFalse();
        softly.assertThat(result.toMarkdown()).contains("no queries defined");
    }

    @Test
    void registerQueryOnce()
    {
        var query = new DummyQuery("q");
        var catalog = new InMemoryQueryCatalog();
        catalog.register(query);
        assertThat(catalog.query("q")).isSameAs(query);
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
        public QueryResult run(QueryBlock queryBlock)
        {
            return failure("Not implemented");
        }
    }
}

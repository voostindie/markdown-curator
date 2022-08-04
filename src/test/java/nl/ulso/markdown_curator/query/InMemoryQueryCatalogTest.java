package nl.ulso.markdown_curator.query;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
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
        var catalog = new InMemoryQueryCatalog(emptySet(), new QueryResultFactory());
        softly.assertThat(catalog.isEmpty()).isTrue();
        var specification = catalog.query("invalid");
        softly.assertThat(specification.name()).isEqualTo("invalid");
        softly.assertThat(specification.description()).contains("Does nothing");
        softly.assertThat(specification.supportedConfiguration()).isEmpty();
        var result = specification.run(emptyQueryBlock());
        softly.assertThat(result.toMarkdown()).contains("no queries defined");
    }

    @Test
    void registerQueriesSameNameOverwrites()
    {
        Set<Query> set = new HashSet<>();
        set.add(new DummyQuery("q"));
        set.add(new DummyQuery("q"));
        var catalog = new InMemoryQueryCatalog(set, new QueryResultFactory());
        assertThat(catalog.queries().size()).isEqualTo(2);
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
            return () -> "Not implemented";
        }
    }
}

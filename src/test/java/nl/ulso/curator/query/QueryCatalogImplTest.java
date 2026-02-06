package nl.ulso.curator.query;

import nl.ulso.curator.change.Changelog;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static nl.ulso.curator.vault.QueryBlockTest.emptyQueryBlock;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class QueryCatalogImplTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void emptyCatalog()
    {
        var catalog = new QueryCatalogImpl(emptySet(), new QueryResultFactoryImpl());
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
        var catalog = new QueryCatalogImpl(set, new QueryResultFactoryImpl());
        assertThat(catalog.queries()).hasSize(2);
    }

    record DummyQuery(String name)
            implements Query
    {
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
            return () -> "Not implemented";
        }
    }
}

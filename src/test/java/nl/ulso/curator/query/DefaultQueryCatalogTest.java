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
class DefaultQueryCatalogTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void emptyCatalog()
    {
        var catalog = new DefaultQueryCatalog(emptySet(), new DefaultQueryResultFactory());
        softly.assertThat(catalog.isEmpty()).isTrue();
        var specification = catalog.query("invalid");
        softly.assertThat(specification.name()).isEqualTo("invalid");
        softly.assertThat(specification.description()).contains("Does nothing");
        softly.assertThat(specification.supportedConfiguration()).isEmpty();
        var result = (ErrorResult) specification.run(emptyQueryBlock());
        softly.assertThat(result.errorMessage()).contains("no queries defined");
    }

    @Test
    void registerQueriesSameNameOverwrites()
    {
        Set<Query> set = new HashSet<>();
        set.add(new DummyQuery("q"));
        set.add(new DummyQuery("q"));
        var catalog = new DefaultQueryCatalog(set, new DefaultQueryResultFactory());
        assertThat(catalog.queries()).hasSize(2);
    }

    @Test
    void replaceDynamicQueries()
    {
        var catalog = new DefaultQueryCatalog(
            Set.of(new DummyQuery("static")), new DefaultQueryResultFactory());
        catalog.replaceDynamicQueries(List.of(new DummyQuery("dynamic1")));
        softly.assertThat(catalog.query("dynamic1")).isInstanceOf(DummyQuery.class);
        catalog.replaceDynamicQueries(List.of(new DummyQuery("dynamic2")));
        softly.assertThat(catalog.query("dynamic1")).isInstanceOf(UnknownQuery.class);
        softly.assertThat(catalog.query("dynamic2")).isInstanceOf(DummyQuery.class);
        softly.assertThat(catalog.query("static")).isInstanceOf(DummyQuery.class);
        softly.assertThat(catalog.queries()).hasSize(3);
        catalog.replaceDynamicQueries(List.of());
        softly.assertThat(catalog.queries()).hasSize(2);
    }

    @Test
    void rejectDynamicQueryThatClashesWithStaticQuery()
    {
        var catalog = new DefaultQueryCatalog(
            Set.of(new DummyQuery("static")), new DefaultQueryResultFactory());
        catalog.replaceDynamicQueries(List.of(new DummyQuery("dynamic")));
        var clash = List.<Query>of(new DummyQuery("other"), new DummyQuery("static"));
        softly.assertThatThrownBy(() -> catalog.replaceDynamicQueries(clash))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("static");
        softly.assertThat(catalog.query("dynamic")).isInstanceOf(DummyQuery.class);
        softly.assertThat(catalog.query("other")).isInstanceOf(UnknownQuery.class);
    }

    @Test
    void rejectDuplicateDynamicQueries()
    {
        var catalog = new DefaultQueryCatalog(emptySet(), new DefaultQueryResultFactory());
        var duplicates = List.<Query>of(new DummyQuery("q"), new DummyQuery("q"));
        softly.assertThatThrownBy(() -> catalog.replaceDynamicQueries(duplicates))
            .isInstanceOf(IllegalArgumentException.class);
        softly.assertThat(catalog.isEmpty()).isTrue();
    }

    @Test
    void helpQueryCannotBeReplaced()
    {
        var catalog = new DefaultQueryCatalog(emptySet(), new DefaultQueryResultFactory());
        var help = List.<Query>of(new DummyQuery("help"));
        softly.assertThatThrownBy(() -> catalog.replaceDynamicQueries(help))
            .isInstanceOf(IllegalArgumentException.class);
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
            return new ErrorResult("Not implemented");
        }
    }
}

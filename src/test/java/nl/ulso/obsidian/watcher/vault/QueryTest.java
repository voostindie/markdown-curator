package nl.ulso.obsidian.watcher.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(SoftAssertionsExtension.class)
class QueryTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(Query.class).withIgnoredFields("lines").verify();
    }

    @Test
    void empty()
    {
        var emptyQuery = new Query(List.of("<!--query-->", "<!--/query-->"));
        softly.assertThat(emptyQuery.isEmpty()).isTrue();
        softly.assertThat(emptyQuery.configuration().isEmpty()).isTrue();
        softly.assertThat(emptyQuery.result()).isBlank();
    }

    @Test
    void singleLineConfigurationNoOutput()
    {
        var emptyQuery = new Query(List.of("<!--query foo: bar-->", "<!--/query-->"));
        softly.assertThat(emptyQuery.isEmpty()).isFalse();
        softly.assertThat(emptyQuery.configuration().string("foo", null)).isEqualTo("bar");
        softly.assertThat(emptyQuery.result()).isBlank();
    }

    @Test
    void multiLineConfigurationNoOutput()
    {
        var query = new Query(
                List.of("<!--query", "foo: bar", "answer: 42", "-->", "<!--/query-->"));
        softly.assertThat(query.isEmpty()).isFalse();
        var configuration = query.configuration();
        softly.assertThat(configuration.string("foo", null)).isEqualTo("bar");
        softly.assertThat(configuration.integer("answer", -1)).isEqualTo(42);
        softly.assertThat(query.result()).isBlank();
    }

    @Test
    void singleLineDefinitionSingleLineOutput()
    {
        var emptyQuery = new Query(
                List.of("<!--query-->", "output", "<!--/query-->"));
        softly.assertThat(emptyQuery.isEmpty()).isFalse();
        softly.assertThat(emptyQuery.configuration().isEmpty()).isTrue();
        softly.assertThat(emptyQuery.result()).isEqualTo("output");
    }

    @Test
    void singleLineDefinitionMultiLineOutput()
    {
        var emptyQuery = new Query(
                List.of("<!--query-->", "line 1", "line 2", "", "<!--/query-->"));
        softly.assertThat(emptyQuery.isEmpty()).isFalse();
        softly.assertThat(emptyQuery.configuration().isEmpty()).isTrue();
        softly.assertThat(emptyQuery.result()).isEqualTo("line 1\nline 2");
    }

    @Test
    void defaultTypeIsCypher()
    {
        var query = new Query(List.of("<!--query-->", "<!--/query-->"));
        softly.assertThat(query.type()).isEqualTo("cypher");
    }

    @Test
    void customType()
    {
        var query = new Query(List.of("<!--query:custom-->", "<!--/query-->"));
        softly.assertThat(query.type()).isEqualTo("custom");
        softly.assertThat(query.configuration().isEmpty()).isTrue();
    }

    @Test
    void customTypeMissingOneLine()
    {
        var query = new Query(List.of("<!--query:-->", "<!--/query-->"));
        softly.assertThat(query.type()).isEqualTo("cypher");
        softly.assertThat(query.configuration().isEmpty()).isTrue();
    }

    @Test
    void customTypeMissingMultiLines()
    {
        var query = new Query(List.of("<!--query:", "foo: bar", "-->", "<!--/query-->"));
        softly.assertThat(query.type()).isEqualTo("cypher");
        softly.assertThat(query.configuration().string("foo", null)).isEqualTo("bar");
    }

    @Test
    void invalidQuery()
    {
        var query = new Query(List.of("<!--query", "<!--/query-->"));
        softly.assertThat(query.type()).isEqualTo("cypher");
        softly.assertThat(query.configuration().isEmpty()).isTrue();
        softly.assertThat(query.result()).isBlank();
    }
}
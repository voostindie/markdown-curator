package nl.ulso.markdown_curator.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static java.util.Collections.emptyList;
import static nl.ulso.markdown_curator.vault.Document.newDocument;

@ExtendWith(SoftAssertionsExtension.class)
public class QueryBlockTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    public static QueryBlock emptyQueryBlock()
    {
        return new QueryBlock(List.of("<!--query-->", "<!--/query-->"));
    }

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(QueryBlock.class)
                .withPrefabValues(Document.class,
                        newDocument("1", 0, emptyList()),
                        newDocument("2", 0, emptyList()))
                .withPrefabValues(Section.class,
                        new Section(1, "1", emptyList()),
                        new Section(1, "2", emptyList()))
                .withIgnoredFields("document", "section")
                .verify();
    }

    @Test
    void empty()
    {
        var emptyQuery = emptyQueryBlock();
        softly.assertThat(emptyQuery.configuration().isEmpty()).isTrue();
        softly.assertThat(emptyQuery.outputHash()).isBlank();
    }

    @Test
    void singleLineConfigurationNoOutput()
    {
        var emptyQuery = new QueryBlock(List.of("<!--query foo: bar-->", "<!--/query-->"));
        softly.assertThat(emptyQuery.configuration().string("foo", null)).isEqualTo("bar");
        softly.assertThat(emptyQuery.outputHash()).isBlank();
    }

    @Test
    void multiLineConfigurationNoOutput()
    {
        var query = new QueryBlock(
                List.of("<!--query", "foo: bar", "answer: 42", "-->", "<!--/query-->"));
        var configuration = query.configuration();
        softly.assertThat(configuration.string("foo", null)).isEqualTo("bar");
        softly.assertThat(configuration.integer("answer", -1)).isEqualTo(42);
        softly.assertThat(query.outputHash()).isBlank();
    }

    @Test
    void singleLineDefinitionSingleLineOutput()
    {
        var emptyQuery = new QueryBlock(
                List.of("<!--query-->", "output", "<!--/query (hash)-->"));
        softly.assertThat(emptyQuery.configuration().isEmpty()).isTrue();
        softly.assertThat(emptyQuery.outputHash()).isEqualTo("hash");
    }

    @Test
    void singleLineDefinitionMultiLineOutput()
    {
        var emptyQuery = new QueryBlock(
                List.of("<!--query-->", "line 1", "line 2", "", "<!--/query (hash)-->"));
        softly.assertThat(emptyQuery.configuration().isEmpty()).isTrue();
        softly.assertThat(emptyQuery.outputHash().isEmpty()).isFalse();
    }

    @Test
    void defaultTypeIsNone()
    {
        var query = new QueryBlock(List.of("<!--query-->", "<!--/query-->"));
        softly.assertThat(query.queryName()).isEqualTo("none");
    }

    @Test
    void customType()
    {
        var query = new QueryBlock(List.of("<!--query:custom-->", "<!--/query-->"));
        softly.assertThat(query.queryName()).isEqualTo("custom");
        softly.assertThat(query.configuration().isEmpty()).isTrue();
    }

    @Test
    void customTypeMissingOneLine()
    {
        var query = new QueryBlock(List.of("<!--query:-->", "<!--/query-->"));
        softly.assertThat(query.queryName()).isEqualTo("none");
        softly.assertThat(query.configuration().isEmpty()).isTrue();
    }

    @Test
    void customTypeMissingMultiLines()
    {
        var query = new QueryBlock(List.of("<!--query:", "foo: bar", "-->", "<!--/query-->"));
        softly.assertThat(query.queryName()).isEqualTo("none");
        softly.assertThat(query.configuration().string("foo", null)).isEqualTo("bar");
    }

    @Test
    void invalidQuery()
    {
        var query = new QueryBlock(List.of("<!--query", "<!--/query-->"));
        softly.assertThat(query.queryName()).isEqualTo("none");
        softly.assertThat(query.configuration().isEmpty()).isTrue();
        softly.assertThat(query.outputHash()).isBlank();
    }

    @Test
    void hashIncluded()
    {
        var query = new QueryBlock(List.of("<!--query:hash-->", "<!--/query (47ef02da)-->"));
        softly.assertThat(query.queryName()).isEqualTo("hash");
        softly.assertThat(query.configuration().isEmpty()).isTrue();
        softly.assertThat(query.outputHash()).isEqualTo("47ef02da");
    }
}
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
        var emptyQuery = new Query(List.of("<!--query-->", "<!--/query-->"), 1);
        softly.assertThat(emptyQuery.isEmpty()).isTrue();
        softly.assertThat(emptyQuery.definition()).isBlank();
        softly.assertThat(emptyQuery.result()).isBlank();
    }

    @Test
    void singleLineDefinitionNoOutput()
    {
        var emptyQuery = new Query(List.of("<!--query on a single line-->", "<!--/query-->"), 1);
        softly.assertThat(emptyQuery.isEmpty()).isFalse();
        softly.assertThat(emptyQuery.definition()).isEqualTo("on a single line");
        softly.assertThat(emptyQuery.result()).isBlank();
    }

    @Test
    void multiLineDefinitionNoOutput()
    {
        var emptyQuery = new Query(
                List.of("<!--query", "on", "multiple", "lines-->", "<!--/query-->"), 4);
        softly.assertThat(emptyQuery.isEmpty()).isFalse();
        softly.assertThat(emptyQuery.definition()).isEqualTo("on\nmultiple\nlines");
        softly.assertThat(emptyQuery.result()).isBlank();
    }

    @Test
    void singleLineDefinitionSingleLineOutput()
    {
        var emptyQuery = new Query(
                List.of("<!--query with-->", "output", "<!--/query-->"), 1);
        softly.assertThat(emptyQuery.isEmpty()).isFalse();
        softly.assertThat(emptyQuery.definition()).isEqualTo("with");
        softly.assertThat(emptyQuery.result()).isEqualTo("output");
    }

    @Test
    void singleLineDefinitionMultiLineOutput()
    {
        var emptyQuery = new Query(
                List.of("<!--query with-->", "", "line 1", "line 2", "", "<!--/query-->"), 1);
        softly.assertThat(emptyQuery.isEmpty()).isFalse();
        softly.assertThat(emptyQuery.definition()).isEqualTo("with");
        softly.assertThat(emptyQuery.result()).isEqualTo("line 1\nline 2");
    }
}
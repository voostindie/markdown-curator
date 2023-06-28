package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.QueryDefinitionStub;
import nl.ulso.markdown_curator.query.QueryResultFactory;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static nl.ulso.markdown_curator.journal.JournalTest.createTestJournal;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class TimelineQueryTest
{
    @Test
    void name()
    {
        var query = new TimelineQuery(null, new QueryResultFactory());
        assertThat(query.name()).isEqualTo("timeline");
    }

    @Test
    void description()
    {
        var query = new TimelineQuery(null, new QueryResultFactory());
        assertThat(query.description()).isNotBlank();
    }

    @Test
    void supportedConfiguration()
    {
        var query = new TimelineQuery(null, new QueryResultFactory());
        assertThat(query.supportedConfiguration().size()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("documentSummaries")
    void timelines(String documentName, String expectedSummary)
    {
        var journal = createTestJournal();
        var vault = (VaultStub) journal.vault();
        var query = new TimelineQuery(journal, new QueryResultFactory());
        var definition = new QueryDefinitionStub(query, vault.addDocument("query", ""))
                .withConfiguration("document", documentName);
        var result = query.run(definition);
        assertThat(result.toMarkdown()).isEqualTo(expectedSummary.trim());
    }

    public static Stream<Arguments> documentSummaries()
    {
        return Stream.of(
                Arguments.of("foo", """
                        - **[[2023-01-27]]**:
                            - [[foo]]
                        - **[[2023-01-26]]**:
                            - [[foo]]
                        - **[[2023-01-25]]**:
                            - [[foo]]
                                                
                        (*3 results*)
                        """),
                Arguments.of("bar", """
                        - **[[2023-01-26]]**:
                            - [[bar]]
                                                
                        (*1 result*)
                        """),
                Arguments.of("nothing", """
                        No results
                        """)
        );
    }
}

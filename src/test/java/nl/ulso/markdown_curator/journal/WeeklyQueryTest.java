package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.QueryDefinitionStub;
import nl.ulso.markdown_curator.query.QueryResultFactory;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.temporal.WeekFields;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyQueryTest
{
    private Journal journal;

    @BeforeEach
    void setUp()
    {
        journal = JournalTest.createTestJournal();
    }

    @Test
    void name()
    {
        var query = createQuery();
        assertThat(query.name()).isEqualTo("weekly");
    }

    @Test
    void description()
    {
        var query = createQuery();
        assertThat(query.description()).isEqualTo(
                "Generates a weekly overview of activities, extracted from the journal");
    }

    @Test
    void supportedConfiguration()
    {
        var query = createQuery();
        assertThat(query.supportedConfiguration()).containsOnlyKeys(
                "folder");
    }

    @ParameterizedTest
    @MethodSource("weeks")
    void testWeek(String documentInPath, String expectedResult)
    {
        var document =
                ((VaultStub) journal.vault()).resolveDocumentInPath(documentInPath);
        var query = createQuery();
        var definition = new QueryDefinitionStub(query, document)
                .withConfiguration("folder", "Projects");
        var result = query.run(definition);
        assertThat(result.toMarkdown().trim()).isEqualTo(expectedResult.trim());
    }

    public static Stream<Arguments> weeks()
    {
        return Stream.of(
                Arguments.of("Journal/2023/2023 Week 04", """
                        - [[bar]]
                        - [[baz]]
                        - [[foo]]
                        """),
                Arguments.of("Journal/2023/2023 Week 05", "No results"),
                Arguments.of("Journal/2023/2023-01-25", "No results")
        );
    }

    private WeeklyQuery createQuery()
    {
        return new WeeklyQuery(journal,
                new JournalSettings("Journal", "Activities", "Projects", WeekFields.ISO),
                new QueryResultFactory());
    }
}

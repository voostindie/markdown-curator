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

class PeriodQueryTest
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
        assertThat(query.name()).isEqualTo("period");
    }

    @Test
    void description()
    {
        var query = createQuery();
        assertThat(query.description()).isEqualTo(
                "Generates an overview of notes touched in a certain period, " +
                "extracted from the journal");
    }

    @Test
    void supportedConfiguration()
    {
        var query = createQuery();
        assertThat(query.supportedConfiguration()).containsOnlyKeys(
                "start", "end", "folder");
    }

    @ParameterizedTest
    @MethodSource("periods")
    void testPeriod(String startDate, String endDate, String expectedResult)
    {
        var document =
                ((VaultStub) journal.vault()).resolveDocumentInPath("Journal/2023/2023 Week 04");
        var query = createQuery();
        var definition = new QueryDefinitionStub(query, document)
                .withConfiguration("folder", "Projects")
                .withConfiguration("start", startDate)
                .withConfiguration("end", endDate);
        var result = query.run(definition);
        assertThat(result.toMarkdown().trim()).isEqualTo(expectedResult.trim());
    }

    public static Stream<Arguments> periods()
    {
        return Stream.of(
                Arguments.of("2020-01-01", "2020-12-31", "No results"),
                Arguments.of("2023-01-25", "2023-01-25", """
                        - [[baz]]
                        - [[foo]]
                        """),
                Arguments.of("2023-01-25", "2023-01-27", """
                        - [[bar]]
                        - [[baz]]
                        - [[foo]]
                        """),
                Arguments.of("invalid", "not-a-date", "No results")
        );
    }

    private PeriodQuery createQuery()
    {
        return new PeriodQuery(journal,
                new JournalSettings("Journal", "Markers", "Activities", "Projects", WeekFields.ISO),
                new QueryResultFactory());
    }
}

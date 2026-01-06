package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.Change;
import nl.ulso.markdown_curator.query.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static nl.ulso.markdown_curator.Change.Kind.CREATE;
import static nl.ulso.markdown_curator.Change.Kind.DELETE;
import static nl.ulso.markdown_curator.Change.Kind.UPDATE;
import static nl.ulso.markdown_curator.Change.create;
import static nl.ulso.markdown_curator.Change.delete;
import static nl.ulso.markdown_curator.Change.update;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static nl.ulso.markdown_curator.journal.JournalBuilder.parseWeeklyFrom;
import static org.assertj.core.api.Assertions.assertThat;

class WeekNavigationQueryTest
{
    @Test
    void name()
    {
        var query = new WeekNavigationQuery(null, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        assertThat(query.name()).isEqualTo("weeknav");
    }

    @Test
    void description()
    {
        var query = new WeekNavigationQuery(null, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        assertThat(query.description()).isNotBlank();
    }

    @Test
    void supportedConfiguration()
    {
        var query = new WeekNavigationQuery(null, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        assertThat(query.supportedConfiguration()).isEmpty();
    }

    @Test
    void weeklyNavigator()
    {
        var journal = JournalTest.createTestJournal();
        var query = new WeekNavigationQuery(journal, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        var result = query.run(new QueryDefinitionStub(query,
                journal.vault().folder("Journal").orElseThrow().folder("2023").orElseThrow()
                        .document("2023 Week 04").orElseThrow()));
        assertThat(result.toMarkdown().trim())
                .isEqualTo("""
                        # [[2023 Week 05|→]] 2023, Week 4
                        
                        ## [[2023-01-25|Wednesday]] | [[2023-01-26|Thursday]] | [[2023-01-27|Friday]]
                        """.trim());
    }

    @Test
    void invalidDocument()
    {
        var journal = JournalTest.createTestJournal();
        var query = new WeekNavigationQuery(journal, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        var result = query.run(new QueryDefinitionStub(query,
                journal.vault().folder("Journal").orElseThrow().folder("2023").orElseThrow()
                        .document("2023-01-25").orElseThrow()));
        assertThat(result.toMarkdown().trim())
                .isEqualTo("""
                        ### Error
                        
                        Document is not a weekly journal!
                        """.trim());
    }

    @ParameterizedTest
    @MethodSource("dailyStream")
    void impactByChangelogWithDailyUpdates(
        String currentWeeklyName, Change.Kind kind, String changedDailyName, boolean expectedImpact)
    {
        var journal = JournalTest.createTestJournal();
        var query = new WeekNavigationQuery(journal, new QueryResultFactory(),
            new ResourceBundledGeneralMessages(Locale.ENGLISH)
        );
        var definition = new QueryDefinitionStub(
            query, journal.vault().findDocument(currentWeeklyName).orElseThrow());
        var changedDaily = journal.toDaily(
                journal.vault().findDocument(changedDailyName).orElseThrow())
            .orElseThrow();
        var change = switch (kind)
        {
            case CREATE -> create(changedDaily, Daily.class);
            case UPDATE -> update(changedDaily, Daily.class);
            case DELETE -> delete(changedDaily, Daily.class);
        };
        var actualImpact = query.isImpactedBy(changelogFor(change), definition);
        assertThat(actualImpact).isEqualTo(expectedImpact);
    }

    public static Stream<Arguments> dailyStream()
    {
        return Stream.of(
            Arguments.of("2023 Week 04", CREATE, "2024-07-21", false),
            Arguments.of("2023 Week 04", CREATE, "2023-01-25", true),
            Arguments.of("2023 Week 04", CREATE, "2023-01-26", true),
            Arguments.of("2023 Week 04", UPDATE, "2023-01-26", false),
            Arguments.of("2023 Week 04", UPDATE, "2024-07-21", false),
            Arguments.of("2023 Week 04", DELETE, "2023-01-26", true),
            Arguments.of("2023 Week 04", DELETE, "2024-07-21", false)
        );
    }

    @ParameterizedTest
    @MethodSource("weeklyStream")
    void impactByChangelogWithWeeklyUpdates(
        String currentWeeklyName, Change.Kind kind, String changedWeeklyName, boolean expectedImpact)
    {
        var journal = JournalTest.createTestJournal();
        var query = new WeekNavigationQuery(journal, new QueryResultFactory(),
            new ResourceBundledGeneralMessages(Locale.ENGLISH)
        );
        var definition = new QueryDefinitionStub(
            query, journal.vault().findDocument(currentWeeklyName).orElseThrow());
        var changedWeekly = parseWeeklyFrom(
            journal.vault().findDocument(changedWeeklyName).orElseThrow())
            .orElseThrow();
        var change = switch (kind)
        {
            case CREATE -> create(changedWeekly, Weekly.class);
            case UPDATE -> update(changedWeekly, Weekly.class);
            case DELETE -> delete(changedWeekly, Weekly.class);
        };
        var actualImpact = query.isImpactedBy(changelogFor(change), definition);
        assertThat(actualImpact).isEqualTo(expectedImpact);
    }

    public static Stream<Arguments> weeklyStream()
    {
        return Stream.of(
            Arguments.of("2023 Week 04", CREATE, "2023 Week 04", true),
            Arguments.of("2023 Week 04", CREATE, "2023 Week 05", true),
            Arguments.of("2023 Week 05", CREATE, "2023 Week 04", true),
            Arguments.of("2023 Week 04", UPDATE, "2023 Week 04", false),
            Arguments.of("2023 Week 05", UPDATE, "2023 Week 04", false),
            Arguments.of("2023 Week 05", DELETE, "2023 Week 04", true)
        );
    }
}

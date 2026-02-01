package nl.ulso.curator.journal;

import nl.ulso.curator.Change;
import nl.ulso.curator.query.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static nl.ulso.curator.Change.Kind.CREATE;
import static nl.ulso.curator.Change.Kind.DELETE;
import static nl.ulso.curator.Change.Kind.UPDATE;
import static nl.ulso.curator.Change.create;
import static nl.ulso.curator.Change.delete;
import static nl.ulso.curator.Change.update;
import static nl.ulso.curator.Changelog.changelogFor;
import static nl.ulso.curator.journal.JournalBuilder.parseWeeklyFrom;
import static org.assertj.core.api.Assertions.assertThat;

class DayNavigationQueryTest
{
    @Test
    void name()
    {
        var query = new DayNavigationQuery(null, new QueryResultFactory(),
            new ResourceBundledGeneralMessages(Locale.ENGLISH)
        );
        assertThat(query.name()).isEqualTo("daynav");
    }

    @Test
    void description()
    {
        var query = new DayNavigationQuery(null, new QueryResultFactory(),
            new ResourceBundledGeneralMessages(Locale.ENGLISH)
        );
        assertThat(query.description()).isNotBlank();
    }

    @Test
    void supportedConfiguration()
    {
        var query = new DayNavigationQuery(null, new QueryResultFactory(),
            new ResourceBundledGeneralMessages(Locale.ENGLISH)
        );
        assertThat(query.supportedConfiguration()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("navigatorStream")
    void dailyNavigators(String daily, Locale locale, String markdownOutput)
    {
        var journal = JournalTest.createTestJournal();
        var query = new DayNavigationQuery(journal, new QueryResultFactory(),
            new ResourceBundledGeneralMessages(locale)
        );
        var result = query.run(new QueryDefinitionStub(query,
            journal.vault().folder("Journal").orElseThrow().folder("2023").orElseThrow()
                .document(daily).orElseThrow()
        ));
        assertThat(result.toMarkdown()).isEqualTo(markdownOutput);
    }

    public static Stream<Arguments> navigatorStream()
    {
        return Stream.of(
            Arguments.of("2023-01-25", Locale.ENGLISH,
                "# [[2023-01-26|→]] [[2023 Week 04|↑]] Wednesday, January 25, 2023"
            ),
            Arguments.of("2023-01-26", Locale.ENGLISH,
                "# [[2023-01-25|←]] [[2023-01-27|→]] [[2023 Week 04|↑]] Thursday, " +
                "January 26, 2023"
            ),
            Arguments.of("2023-01-27", Locale.ENGLISH,
                "# [[2023-01-26|←]] [[2023-12-25|→]] [[2023 Week 04|↑]] Friday, January 27, 2023"
            ),
            Arguments.of("2023-01-27", Locale.forLanguageTag("nl"),
                "# [[2023-01-26|←]] [[2023-12-25|→]] [[2023 Week 04|↑]] Vrijdag 27 januari 2023"
            ),
            Arguments.of("2023-12-25", Locale.ENGLISH,
                "# [[2023-01-27|←]] [[2024-07-21|→]] Monday, December 25, 2023"
            )
        );
    }

    @Test
    void invalidDocument()
    {
        var journal = JournalTest.createTestJournal();
        var query = new DayNavigationQuery(journal, new QueryResultFactory(),
            new ResourceBundledGeneralMessages(Locale.ENGLISH)
        );
        var result = query.run(new QueryDefinitionStub(query,
            journal.vault().folder("Journal").orElseThrow().folder("2023").orElseThrow()
                .document("2023 Week 04").orElseThrow()
        ));
        assertThat(result.toMarkdown().trim())
            .isEqualTo("""
                ### Error
                
                Document is not a daily journal!
                """.trim());
    }

    @ParameterizedTest
    @MethodSource("dailyStream")
    void impactByChangelogWithDailyUpdates(
        String currentDailyName, Change.Kind kind, String changedDailyName, boolean expectedImpact)
    {
        var journal = JournalTest.createTestJournal();
        var query = new DayNavigationQuery(journal, new QueryResultFactory(),
            new ResourceBundledGeneralMessages(Locale.ENGLISH)
        );
        var definition = new QueryDefinitionStub(
            query, journal.vault().findDocument(currentDailyName).orElseThrow());
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
            Arguments.of("2023-01-25", CREATE, "2024-07-21", false),
            Arguments.of("2024-07-21", CREATE, "2023-01-25", false),
            Arguments.of("2023-01-25", CREATE, "2023-01-25", true),
            Arguments.of("2023-01-25", CREATE, "2023-01-26", true),
            Arguments.of("2023-01-26", UPDATE, "2023-01-26", false),
            Arguments.of("2023-01-26", UPDATE, "2023-01-25", false),
            Arguments.of("2023-01-26", UPDATE, "2023-01-27", false),
            Arguments.of("2023-01-25", DELETE, "2023-01-26", true),
            Arguments.of("2023-01-25", DELETE, "2024-07-21", false),
            Arguments.of("2023-01-26", DELETE, "2023-01-25", true),
            Arguments.of("2024-07-21", CREATE, "2024-08-11", true)
        );
    }

    @ParameterizedTest
    @MethodSource("weeklyStream")
    void impactByChangelogWithWeeklyUpdates(
        String currentDailyName, Change.Kind kind, String changedWeeklyName, boolean expectedImpact)
    {
        var journal = JournalTest.createTestJournal();
        var query = new DayNavigationQuery(journal, new QueryResultFactory(),
            new ResourceBundledGeneralMessages(Locale.ENGLISH)
        );
        var definition = new QueryDefinitionStub(
            query, journal.vault().findDocument(currentDailyName).orElseThrow());
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
            Arguments.of("2023-01-25", CREATE, "2023 Week 04", true),
            Arguments.of("2023-01-25", CREATE, "2023 Week 05", false),
            Arguments.of("2023-01-26", UPDATE, "2023 Week 04", false),
            Arguments.of("2023-01-26", UPDATE, "2023 Week 04", false),
            Arguments.of("2023-01-26", UPDATE, "2023 Week 04", false),
            Arguments.of("2023-01-25", DELETE, "2023 Week 04", true),
            Arguments.of("2023-01-25", DELETE, "2023 Week 05", false)
        );
    }
}

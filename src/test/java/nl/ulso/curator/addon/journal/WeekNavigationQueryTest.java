package nl.ulso.curator.addon.journal;

import nl.ulso.curator.change.Change;
import nl.ulso.curator.query.QueryDefinitionStub;
import nl.ulso.curator.vault.VaultStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Locale.ENGLISH;
import static nl.ulso.curator.addon.journal.JournalTest.createTestJournal;
import static nl.ulso.curator.addon.journal.Weekly.parseWeeklyFrom;
import static nl.ulso.curator.change.Change.Kind.CREATE;
import static nl.ulso.curator.change.Change.Kind.DELETE;
import static nl.ulso.curator.change.Change.Kind.UPDATE;
import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.query.QueryTestModule.createQueryResultFactory;
import static org.assertj.core.api.Assertions.assertThat;

class WeekNavigationQueryTest
{
    @Test
    void name()
    {
        var query = new WeekNavigationQuery(null, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        assertThat(query.name()).isEqualTo("weeknav");
    }

    @Test
    void description()
    {
        var query = new WeekNavigationQuery(null, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        assertThat(query.description()).isNotBlank();
    }

    @Test
    void supportedConfiguration()
    {
        var query = new WeekNavigationQuery(null, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        assertThat(query.supportedConfiguration()).isEmpty();
    }

    @Test
    void weeklyNavigator()
    {
        var vault = new VaultStub();
        var journal = createTestJournal(vault);
        var query = new WeekNavigationQuery(journal, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        var result = query.run(new QueryDefinitionStub(query,
            vault.folder("Journal").orElseThrow().folder("2023").orElseThrow()
                .document("2023 Week 04").orElseThrow()
        ));
        assertThat(result.toMarkdown().trim())
            .isEqualTo("""
                # [[2023 Week 05|→]] 2023, Week 4
                
                ## [[2023-01-25|Wednesday]] | [[2023-01-26|Thursday]] | [[2023-01-27|Friday]]
                """.trim());
    }

    @Test
    void invalidDocument()
    {
        var vault = new VaultStub();
        var journal = createTestJournal(vault);
        var query = new WeekNavigationQuery(journal, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        var result = query.run(new QueryDefinitionStub(query,
            vault.folder("Journal").orElseThrow().folder("2023").orElseThrow()
                .document("2023-01-25").orElseThrow()
        ));
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
        var vault = new VaultStub();
        var journal = createTestJournal(vault);
        var query = new WeekNavigationQuery(journal, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        var definition = new QueryDefinitionStub(
            query, vault.findDocument(currentWeeklyName).orElseThrow());
        var changedDaily = journal.toDaily(
                vault.findDocument(changedDailyName).orElseThrow())
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
        String currentWeeklyName, Change.Kind kind, String changedWeeklyName,
        boolean expectedImpact)
    {
        var vault = new VaultStub();
        var journal = createTestJournal(vault);
        var query = new WeekNavigationQuery(journal, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        var definition = new QueryDefinitionStub(
            query, vault.findDocument(currentWeeklyName).orElseThrow());
        var changedWeekly = parseWeeklyFrom(
            vault.findDocument(changedWeeklyName).orElseThrow())
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

    private static JournalMessages createMessages(Locale locale)
    {
        return new ResourceBundleJournalMessages(Optional.of(locale));
    }
}

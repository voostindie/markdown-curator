package nl.ulso.curator.addon.journal;

import nl.ulso.curator.changelog.Change;
import nl.ulso.curator.changelog.Changelog;
import nl.ulso.curator.query.QueryDefinitionStub;
import nl.ulso.curator.vault.VaultStub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static java.util.Locale.ENGLISH;
import static nl.ulso.curator.addon.journal.JournalTest.createTestJournal;
import static nl.ulso.curator.changelog.Change.Kind.CREATE;
import static nl.ulso.curator.changelog.Change.Kind.DELETE;
import static nl.ulso.curator.changelog.Change.Kind.UPDATE;
import static nl.ulso.curator.changelog.Change.create;
import static nl.ulso.curator.changelog.Change.delete;
import static nl.ulso.curator.changelog.Change.update;
import static nl.ulso.curator.query.QueryModuleTest.createMessages;
import static nl.ulso.curator.query.QueryModuleTest.createQueryResultFactory;
import static org.assertj.core.api.Assertions.assertThat;

class LatestJournalNavigationQueryTest
{
    @Test
    void name()
    {
        var query = new LatestJournalNavigationQuery(null, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        assertThat(query.name()).isEqualTo("latestnav");
    }

    @Test
    void description()
    {
        var query = new LatestJournalNavigationQuery(null, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        assertThat(query.description()).isNotBlank();
    }

    @Test
    void supportedConfiguration()
    {
        var query = new LatestJournalNavigationQuery(null, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        assertThat(query.supportedConfiguration()).containsOnlyKeys("prefix", "postfix");
    }

    @Test
    void latestInTestJournal()
    {
        var journal = createTestJournal();
        var query = new LatestJournalNavigationQuery(journal, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        var result = query.run(new QueryDefinitionStub(query, null));
        assertThat(result.toMarkdown()).isEqualTo("[[2024-08-12|🗓️ Latest]]");
    }

    @Test
    void latestInTestJournalPrefixAndPostfix()
    {
        var journal = createTestJournal();
        var query = new LatestJournalNavigationQuery(journal, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        var result = query.run(
            new QueryDefinitionStub(query, null).withConfiguration("prefix", ">> ")
                .withConfiguration("postfix", " <<"));
        assertThat(result.toMarkdown()).isEqualTo(">> [[2024-08-12|🗓️ Latest]] <<");
    }

    @Test
    void latestInEmptyJournal()
    {
        var vault = new VaultStub();
        var journal = new Journal(vault,
            new JournalSettings("Journal", "Markers", "Activities", "Projects")
        );
        var query = new LatestJournalNavigationQuery(journal, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        var result = query.run(new QueryDefinitionStub(query, null));
        assertThat(result.toMarkdown()).isEqualTo("No results");
    }

    @ParameterizedTest
    @MethodSource("dailyStream")
    void impactIfLatestHasChanged(String dailyName, Change.Kind kind, boolean expectedImpact)
    {
        var journal = createTestJournal();
        var query = new LatestJournalNavigationQuery(journal, createQueryResultFactory(),
            createMessages(ENGLISH)
        );
        var daily = journal.toDaily(journal.vault().findDocument(dailyName).orElseThrow())
            .orElseThrow();
        var change = switch (kind)
        {
            case CREATE -> create(daily, Daily.class);
            case UPDATE -> update(daily, Daily.class);
            case DELETE -> delete(daily, Daily.class);
        };
        var result = query.isImpactedBy(
            Changelog.changelogFor(change),
            new QueryDefinitionStub(query, null)
        );
        assertThat(result).isEqualTo(expectedImpact);
    }

    public static Stream<Arguments> dailyStream()
    {
        return Stream.of(
            Arguments.of("2024-08-12", CREATE, true),
            Arguments.of("2024-08-12", DELETE, true),
            Arguments.of("2024-08-12", UPDATE, false),
            Arguments.of("2024-07-21", CREATE, false),
            Arguments.of("2024-07-21", DELETE, false),
            Arguments.of("2024-07-21", UPDATE, false)
        );

    }

}

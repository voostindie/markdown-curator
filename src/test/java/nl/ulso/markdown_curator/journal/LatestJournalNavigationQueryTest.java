package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static nl.ulso.markdown_curator.journal.JournalTest.createTestJournal;
import static org.assertj.core.api.Assertions.assertThat;

class LatestJournalNavigationQueryTest
{
    @Test
    void name()
    {
        var query = new LatestJournalNavigationQuery(null, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        assertThat(query.name()).isEqualTo("latestnav");
    }

    @Test
    void description()
    {
        var query = new LatestJournalNavigationQuery(null, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        assertThat(query.description()).isNotBlank();
    }

    @Test
    void supportedConfiguration()
    {
        var query = new LatestJournalNavigationQuery(null, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        assertThat(query.supportedConfiguration()).containsOnlyKeys("prefix", "postfix");
    }

    @Test
    void latestInTestJournal()
    {
        var journal = createTestJournal();
        var query = new LatestJournalNavigationQuery(journal, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        var result = query.run(new QueryDefinitionStub(query, null));
        assertThat(result.toMarkdown()).isEqualTo("[[2024-08-12|🗓️ Latest]]");
    }

    @Test
    void latestInTestJournalPrefixAndPostfix()
    {
        var journal = createTestJournal();
        var query = new LatestJournalNavigationQuery(journal, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
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
                new JournalSettings("Journal", "Markers", "Activities", "Projects"));
        var query = new LatestJournalNavigationQuery(journal, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        var result = query.run(new QueryDefinitionStub(query, null));
        assertThat(result.toMarkdown()).isEqualTo("No results");
    }
}

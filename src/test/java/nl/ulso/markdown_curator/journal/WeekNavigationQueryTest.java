package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.*;
import org.junit.jupiter.api.Test;

import java.util.Locale;

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
}

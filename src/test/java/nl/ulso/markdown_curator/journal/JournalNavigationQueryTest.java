package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.QueryDefinitionStub;
import nl.ulso.markdown_curator.query.QueryResultFactory;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class JournalNavigationQueryTest
{
    @Test
    void name()
    {
        var query = new JournalNavigationQuery(null, new QueryResultFactory(), Locale.ENGLISH);
        assertThat(query.name()).isEqualTo("navigator");
    }

    @Test
    void description()
    {
        var query = new JournalNavigationQuery(null, new QueryResultFactory(), Locale.ENGLISH);
        assertThat(query.description()).isNotBlank();
    }

    @Test
    void supportedConfiguration()
    {
        var query = new JournalNavigationQuery(null, new QueryResultFactory(), Locale.ENGLISH);
        assertThat(query.supportedConfiguration()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("navigatorStream")
    void navigators(String journalEntry, Locale locale, String markdownOutput)
    {
        var journal = JournalTest.createTestJournal();
        var query = new JournalNavigationQuery(journal, new QueryResultFactory(), locale);
        var result = query.run(new QueryDefinitionStub(query,
                journal.vault().folder("Journal").orElseThrow().folder("2023").orElseThrow()
                        .document(journalEntry).orElseThrow()));
        assertThat(result.toMarkdown()).isEqualTo(markdownOutput);
    }

    public static Stream<Arguments> navigatorStream()
    {
        return Stream.of(
                Arguments.of("2023-01-25", Locale.ENGLISH,
                        "# [[2023-01-26|➡️]] Wednesday, January 25, 2023"),
                Arguments.of("2023-01-26", Locale.ENGLISH,
                        "# [[2023-01-25|⬅️]] [[2023-01-27|➡️]] Thursday, January 26, 2023"),
                Arguments.of("2023-01-27", Locale.ENGLISH,
                        "# [[2023-01-26|⬅️]] Friday, January 27, 2023"),
                Arguments.of("2023-01-27", Locale.forLanguageTag("nl"),
                        "# [[2023-01-26|⬅️]] vrijdag 27 januari 2023")
        );
    }

}

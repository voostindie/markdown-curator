package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.query.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DayNavigationQueryTest
{
    @Test
    void name()
    {
        var query = new DayNavigationQuery(null, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        assertThat(query.name()).isEqualTo("daynav");
    }

    @Test
    void description()
    {
        var query = new DayNavigationQuery(null, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        assertThat(query.description()).isNotBlank();
    }

    @Test
    void supportedConfiguration()
    {
        var query = new DayNavigationQuery(null, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        assertThat(query.supportedConfiguration()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("navigatorStream")
    void dailyNavigators(String daily, Locale locale, String markdownOutput)
    {
        var journal = JournalTest.createTestJournal();
        var query = new DayNavigationQuery(journal, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(locale));
        var result = query.run(new QueryDefinitionStub(query,
                journal.vault().folder("Journal").orElseThrow().folder("2023").orElseThrow()
                        .document(daily).orElseThrow()));
        assertThat(result.toMarkdown()).isEqualTo(markdownOutput);
    }

    public static Stream<Arguments> navigatorStream()
    {
        return Stream.of(
                Arguments.of("2023-01-25", Locale.ENGLISH,
                        "# [[2023-01-26|→]] [[2023 Week 04|↑]] Wednesday, January 25, 2023"),
                Arguments.of("2023-01-26", Locale.ENGLISH,
                        "# [[2023-01-25|←]] [[2023-01-27|→]] [[2023 Week 04|↑]] Thursday, " +
                        "January 26, 2023"),
                Arguments.of("2023-01-27", Locale.ENGLISH,
                        "# [[2023-01-26|←]] [[2023-12-25|→]] [[2023 Week 04|↑]] Friday, January 27, 2023"),
                Arguments.of("2023-01-27", Locale.forLanguageTag("nl"),
                        "# [[2023-01-26|←]] [[2023-12-25|→]] [[2023 Week 04|↑]] Vrijdag 27 januari 2023"),
                Arguments.of("2023-12-25", Locale.ENGLISH,
                        "# [[2023-01-27|←]] [[2024-07-21|→]] Monday, December 25, 2023")
        );
    }

    @Test
    void invalidDocument()
    {
        var journal = JournalTest.createTestJournal();
        var query = new DayNavigationQuery(journal, new QueryResultFactory(),
                new ResourceBundledGeneralMessages(Locale.ENGLISH));
        var result = query.run(new QueryDefinitionStub(query,
                journal.vault().folder("Journal").orElseThrow().folder("2023").orElseThrow()
                        .document("2023 Week 04").orElseThrow()));
        assertThat(result.toMarkdown().trim())
                .isEqualTo("""
                        ### Error
                        
                        Document is not a daily journal!
                        """.trim());
    }
}

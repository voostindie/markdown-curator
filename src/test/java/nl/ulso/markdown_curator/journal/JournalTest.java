package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.vault.VaultStub;
import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class JournalTest
{
    @ParameterizedTest
    @MethodSource("timelineEntries")
    void timeline(String documentName, Map<LocalDate, String> expected)
    {
        var journal = createTestJournal();
        assertThat(journal.timelineFor(documentName)).containsExactlyInAnyOrderEntriesOf(expected);
    }

    @ParameterizedTest
    @MethodSource("timelineEntries")
    void mostRecent(String documentName, Map<LocalDate, String> expected)
    {
        var expectedDate = expected.keySet().stream().max(Comparator.naturalOrder());
        var journal = createTestJournal();
        assertThat(journal.mostRecentMentionOf(documentName)).isEqualTo(expectedDate);
    }

    @Test
    void addDocument()
    {
        var journal = createTestJournal();
        var vault = (VaultStub) journal.vault();
        var document = vault.addDocumentInPath("Journal/2023-01-28", """
                ## Log
                                
                - [[foo]]
                """);
        journal.process(VaultChangedEvent.documentAdded(document));
        assertThat(journal.timelineFor("foo")).hasSize(4);
    }

    @Test
    void changeDocument()
    {
        var journal = createTestJournal();
        var vault = (VaultStub) journal.vault();
        var document = vault.addDocumentInPath("Journal/2023-01-25", """
                ## Log
                                
                - [[foo]]
                """);
        journal.process(VaultChangedEvent.documentChanged(document));
        assertThat(journal.timelineFor("baz")).hasSize(1);
    }

    @Test
    void removeDocument()
    {
        var journal = createTestJournal();
        var vault = (VaultStub) journal.vault();
        var document = vault.resolveDocumentInPath("Journal/2023-01-25");
        journal.process(VaultChangedEvent.documentRemoved(document));
        assertThat(journal.timelineFor("foo")).hasSize(2);
    }

    public static Stream<Arguments> timelineEntries()
    {
        return Stream.of(
                Arguments.of("foo", Map.of(
                        LocalDate.parse("2023-01-25"), "- [[foo]]\n",
                        LocalDate.parse("2023-01-26"), "- [[foo]]\n",
                        LocalDate.parse("2023-01-27"), "- [[foo]]\n")
                ),
                Arguments.of("bar", Map.of(
                        LocalDate.parse("2023-01-26"), "- [[bar]]\n")
                ),
                Arguments.of("baz", Map.of(
                        LocalDate.parse("2023-01-25"), "- [[baz]]\n",
                        LocalDate.parse("2023-01-27"), "- [[baz]]\n")
                )
        );
    }

    static Journal createTestJournal()
    {
        var vault = new VaultStub();
        vault.addDocumentInPath("Journal/2023-01-25", """
                ## Log
                                
                - [[foo]]
                - [[baz]]
                """);
        vault.addDocumentInPath("Journal/2023-01-26", """
                ## Log
                                
                - [[foo]]
                - [[bar]]
                """);
        vault.addDocumentInPath("Journal/2023-01-27", """
                ## Log
                                
                - [[foo]]
                - [[baz]]
                """);
        var journal = new Journal(vault, new JournalSettings("Journal", "Log"));
        journal.fullRefresh();
        return journal;
    }

}

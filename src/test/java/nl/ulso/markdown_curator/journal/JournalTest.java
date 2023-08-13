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
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static java.time.temporal.WeekFields.ISO;
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
        var document = vault.addDocumentInPath("Journal/2023/2023-01-28", """
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
        var document = vault.addDocumentInPath("Journal/2023/2023-01-25", """
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
        var document = vault.resolveDocumentInPath("Journal/2023/2023-01-25");
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

    @Test
    void referencedDocuments()
    {
        var journal = createTestJournal();
        var entries = journal.entriesUntilIncluding(
                LocalDate.of(2023, 1, 25),
                LocalDate.of(2023, 1, 27)).toList();
        var references = journal.referencedDocumentsIn(entries);
        assertThat(references).containsExactlyInAnyOrder("foo", "bar", "baz");
    }

    static Journal createTestJournal()
    {
        var vault = new VaultStub();
        vault.addDocumentInPath("Projects/foo", "Project 'foo'");
        vault.addDocumentInPath("Projects/bar", "Project 'bar'");
        vault.addDocumentInPath("Projects/baz", "Project 'baz'");
        vault.addDocumentInPath("Journal/2023/2023-01-25", """
                ## Log
                                
                - [[foo]]
                - [[baz]]
                """);
        vault.addDocumentInPath("Journal/2023/2023-01-26", """
                ## Log
                                
                - [[foo]]
                - [[bar]]
                """);
        vault.addDocumentInPath("Journal/2023/2023-01-27", """
                ## Log
                                
                - [[foo]]
                - [[baz]]
                """);
        vault.addDocumentInPath("Journal/2023/2023 Week 04", "");
        vault.addDocumentInPath("Journal/2023/2023 Week 05", "");
        var journal = new Journal(vault, new JournalSettings("Journal", "Log", "Projects",
                WeekFields.ISO));
        journal.fullRefresh();
        return journal;
    }

}

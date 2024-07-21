package nl.ulso.markdown_curator.journal;

import nl.ulso.markdown_curator.vault.VaultStub;
import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
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
        assertThat(journal.timelineFor("baz")).hasSize(2);
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
                        LocalDate.parse("2023-01-25"), """
                                - [[foo]]
                                    - [[❗️]] Remember this
                                """,
                        LocalDate.parse("2023-01-26"), """
                                - [[foo]]
                                    - [[❗️|Important!]] Remember this too
                                """,
                        LocalDate.parse("2023-01-27"), """
                                - [[foo]]
                                    - [[❓]] Important question!
                                """)
                ),
                Arguments.of("bar", Map.of(
                        LocalDate.parse("2023-01-26"), "- [[bar]]\n",
                        LocalDate.parse("2024-07-21"), """
                                - [[bar]]
                                    - [[❗️]] baR marker
                                    - [[baz]]
                                        - [[❗️]] baZ marker
                                    - [[❌]] Special marker
                                """)
                ),
                Arguments.of("baz", Map.of(
                        LocalDate.parse("2023-01-25"), "- [[baz]]\n",
                        LocalDate.parse("2023-01-27"), "- [[baz]]\n",
                        LocalDate.parse("2024-07-21"), """
                                - [[bar]]
                                    - [[baz]]
                                        - [[❗️]] baZ marker
                                """)
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
        assertThat(references).containsExactlyInAnyOrder("foo", "bar", "baz", "❗️", "❓");
    }

    @Test
    void emptyJournalIsIncluded()
    {
        var journal = createTestJournal();
        var entries = journal.entriesUntilIncluding(
                LocalDate.of(2023, 12, 25),
                LocalDate.of(2023, 12, 25)).toList();
        assertThat(entries.size()).isEqualTo(1);
        assertThat(journal.dailyAfter(LocalDate.of(2013, 1, 27))).isPresent();
    }

    @Test
    void documentSections()
    {
        var journal = createTestJournal();
        var markedLines = journal.markedLinesFor("foo", Set.of("❗️", "❓"));
        var importantLines = Strings.join(markedLines.get("❗️")).with("\n");
        var questionLines = Strings.join(markedLines.get("❓")).with("\n");
        assertThat(importantLines).isEqualTo("- Remember this\n- Remember this too");
        assertThat(questionLines).isEqualTo("- Important question!");
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
                    - [[❗️]] Remember this
                - [[baz]]
                """);
        vault.addDocumentInPath("Journal/2023/2023-01-26", """
                ## Log
                
                - [[foo]]
                    - [[❗️|Important!]] Remember this too
                - [[bar]]
                """);
        vault.addDocumentInPath("Journal/2023/2023-01-27", """
                ## Log
                
                - [[foo]]
                    - [[❓]] Important question!
                - [[baz]]
                """);
        vault.addDocumentInPath("Journal/2023/2023 Week 04", "");
        vault.addDocumentInPath("Journal/2023/2023 Week 05", "");
        vault.addDocumentInPath("Journal/2023/2023-12-25", """
                ## No log
                
                No entries in this one!
                """);
        vault.addDocumentInPath("Journal/2024/2024-07-21", """
                ## Log
                
                - [[bar]]
                    - [[❗️]] baR marker
                    - [[baz]]
                        - [[❗️]] baZ marker
                    - [[❌]] Special marker
                """);
        vault.addDocumentInPath("Journal/Markers/❌", """
                ---
                title: CUSTOM TITLE
                ---
                """);
        var journal =
                new Journal(vault, new JournalSettings("Journal", "Markers", "Log", "Projects",
                        WeekFields.ISO));
        journal.fullRefresh();
        return journal;
    }

}

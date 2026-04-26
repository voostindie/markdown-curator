package nl.ulso.curator.addon.journal;

import nl.ulso.curator.vault.VaultStub;
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

import static nl.ulso.curator.main.VaultTestSupport.initializeVault;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class JournalTest
{
    @ParameterizedTest
    @MethodSource("timelineEntries")
    void timeline(String documentName, Map<LocalDate, String> expected)
    {
        var journal = createTestJournal(new VaultStub());
        assertThat(journal.timelineFor(documentName)).containsExactlyInAnyOrderEntriesOf(expected);
    }

    @ParameterizedTest
    @MethodSource("timelineEntries")
    void mostRecent(String documentName, Map<LocalDate, String> expected)
    {
        var expectedDate = expected.keySet().stream().max(Comparator.naturalOrder());
        var journal = createTestJournal(new VaultStub());
        assertThat(journal.mostRecentMentionOf(documentName)).isEqualTo(expectedDate);
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
                        """
                )
            ),
            Arguments.of("bar", Map.of(
                    LocalDate.parse("2023-01-26"), "- [[bar]]\n",
                    LocalDate.parse("2024-07-21"), """
                        - [[bar]]
                            - [[❗️]] baR marker
                            - [[baz]]
                                - [[❗️]] baZ marker
                            - [[❌]] Special marker
                        """
                )
            ),
            Arguments.of("baz", Map.of(
                    LocalDate.parse("2023-01-25"), "- [[baz]]\n",
                    LocalDate.parse("2023-01-27"), "- [[baz]]\n",
                    LocalDate.parse("2024-07-21"), """
                        - [[bar]]
                            - [[baz]]
                                - [[❗️]] baZ marker
                        """
                )
            )
        );
    }

    @Test
    void referencedDocuments()
    {
        var journal = createTestJournal(new VaultStub());
        var entries = journal.dailiesInPeriod(
            LocalDate.of(2023, 1, 25),
            LocalDate.of(2023, 1, 27)
        ).toList();
        var references = journal.referencedDocumentsIn(entries);
        assertThat(references).containsExactlyInAnyOrder("foo", "bar", "baz", "❗️", "❓");
    }

    @Test
    void emptyJournalIsIncluded()
    {
        var journal = createTestJournal(new VaultStub());
        var entries = journal.dailiesInPeriod(
            LocalDate.of(2023, 12, 25),
            LocalDate.of(2023, 12, 25)
        ).toList();
        assertThat(entries).hasSize(1);
    }

    @Test
    void documentSections()
    {
        var journal = createTestJournal(new VaultStub());
        var markedLines = journal.markedLinesFor("foo", Set.of("❗️", "❓"));
        var importantLines =
            Strings.join(markedLines.get("❗️").stream().map(MarkedLine::line).toList())
                .with("\n");
        var questionLines =
            Strings.join(markedLines.get("❓").stream().map(MarkedLine::line).toList())
                .with("\n");
        assertThat(importantLines).isEqualTo("- Remember this\n- Remember this too");
        assertThat(questionLines).isEqualTo("- Important question!");
    }

    @Test
    void documentSectionsForOneDay()
    {
        var journal = createTestJournal(new VaultStub());
        var markedLines = journal.markedLinesFor("foo", Set.of("❗️"), LocalDate.of(2023, 1, 25));
        var lines = Strings.join(markedLines.get("❗️").stream().map(MarkedLine::line).toList())
            .with("\n");
        assertThat(lines).isEqualTo("- Remember this");
    }

    @Test
    void documentSectionsForOneDayNoDaily()
    {
        var journal = createTestJournal(new VaultStub());
        var markedLines = journal.markedLinesFor("foo", Set.of("❗️"), LocalDate.of(2025, 1, 12));
        assertThat(markedLines).isEmpty();
    }

    @Test
    void documentSectionsForOneDayNoEntries()
    {
        var journal = createTestJournal(new VaultStub());
        var markedLines = journal.markedLinesFor("bar", Set.of("❗️"), LocalDate.of(2023, 1, 25));
        assertThat(markedLines).isEmpty();
    }

    @Test
    void latestJournalEntry()
    {
        var journal = createTestJournal(new VaultStub());
        var latest = journal.latest();
        assertThat(latest.get().date()).isEqualTo(LocalDate.of(2024, 8, 12));
    }

    @Test
    void latestJournalEntryInEmptyJournal()
    {
        var settings = new JournalSettings("Journal", "Markers", "Activities", "Projects");
        var journal = new DefaultJournal(
            new DefaultDailyRepository(settings),
            new DefaultWeeklyRepository(settings),
            new DefaultMarkerRepository(settings)
        );
        var latest = journal.latest();
        assertThat(latest).isEmpty();
    }

    @Test
    void markers()
    {
        var journal = createTestJournal(new VaultStub());
        var markers = journal.markers();
        assertThat(markers.keySet()).containsExactly("❌", "🪵");
    }

    @Test
    void validMarkerDocument()
    {
        var journal = createTestJournal(new VaultStub());
        var markers = journal.markers();
        var marker = markers.values().stream().findFirst().get();
        assertThat(journal.isMarkerDocument(marker.document())).isTrue();
    }

    @Test
    void invalidMarkerDocument()
    {
        var vault = new VaultStub();
        var journal = createTestJournal(vault);
        var project = vault.folder("Projects").get().document("foo").get();
        assertThat(journal.isMarkerDocument(project)).isFalse();
    }

    static Journal createTestJournal(VaultStub vault)
    {
        vault.addDocumentInPath("Projects/foo", "Project 'foo'");
        vault.addDocumentInPath("Projects/bar", "Project 'bar'");
        vault.addDocumentInPath("Projects/baz", "Project 'baz'");
        vault.addDocumentInPath("Journal/2023/2023-01-25", """
            ## Log
            
            - [[foo]]
                - [[❗️]] Remember this
            - [[baz]]
            """
        );
        vault.addDocumentInPath("Journal/2023/2023-01-26", """
            ## Log
            
            - [[foo]]
                - [[❗️|Important!]] Remember this too
            - [[bar]]
            """
        );
        vault.addDocumentInPath("Journal/2023/2023-01-27", """
            ## Log
            
            - [[foo]]
                - [[❓]] Important question!
            - [[baz]]
            """
        );
        vault.addDocumentInPath("Journal/2023/2023 Week 04", "");
        vault.addDocumentInPath("Journal/2023/2023 Week 05", "");
        vault.addDocumentInPath("Journal/2023/2023-12-25", """
            ## No log
            
            No entries in this one!
            """
        );
        vault.addDocumentInPath("Journal/2024/2024-07-21", """
            ## Log
            
            - [[bar]]
                - [[❗️]] baR marker
                - [[baz]]
                    - [[❗️]] baZ marker
                - [[❌]] Special marker
            """
        );
        vault.addDocumentInPath("Journal/Markers/❌", """
            ---
            title: CUSTOM TITLE
            ---
            """
        );
        vault.addDocumentInPath("Journal/Markers/🪵", """
            ---
            title: Status log
            group-by-date: true
            ---
            """
        );
        vault.addDocumentInPath("Journal/2024/2024-08-11", """
            ## Log
            
            - [[Project 42]]
                - [[🪵]] Entry one
                - [[🪵]] Entry two
            """
        );
        vault.addDocumentInPath("Journal/2024/2024-08-12", """
            ## Log
            
            - [[Project 42]]
                - [[🪵]] Entry three
            """
        );
        vault.addDocumentInPath("Projects/Project 42", """
            <!--query:marked markers: [🪵]-->
            <!--/query-->
            """
        );
        var settings = new JournalSettings(
            "Journal",
            "Markers",
            "Log",
            "Projects",
            WeekFields.ISO
        );
        var changelog = initializeVault(vault);
        var dailyRepository = new DefaultDailyRepository(settings);
        dailyRepository.apply(changelog);
        var weeklyRepository = new DefaultWeeklyRepository(settings);
        weeklyRepository.apply(changelog);
        var markerRepository = new DefaultMarkerRepository(settings);
        markerRepository.apply(changelog);
        return new DefaultJournal(
            dailyRepository,
            weeklyRepository,
            markerRepository
        );
    }
}

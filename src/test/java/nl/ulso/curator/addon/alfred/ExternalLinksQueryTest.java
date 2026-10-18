package nl.ulso.curator.addon.alfred;

import nl.ulso.curator.addon.journal.*;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.VaultStub;
import nl.ulso.dictionary.Dictionary;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.emptySortedMap;
import static java.util.Optional.empty;
import static nl.ulso.curator.change.Changelog.emptyChangelog;
import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;
import static nl.ulso.curator.query.QueryTestModule.createQueryResultFactory;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ExternalLinksQueryTest
{
    private final QueryResultFormatterRegistry formatterRegistry =
        QueryTestModule.createQueryResultFormatterRegistry();
    private VaultStub vault;
    private Journal journal;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        journal = JournalTest.createTestJournal(vault);
    }

    @Test
    void name()
    {
        var query = createQuery(null);
        assertThat(query.name()).isEqualTo("links");
    }

    @Test
    void description()
    {
        var query = createQuery(null);
        assertThat(query.description()).isEqualTo(
            "Gathers all external links for a document, for use in the HTTP server only.");
    }

    @Test
    void supportedConfiguration()
    {
        var query = createQuery(null);
        assertThat(query.supportedConfiguration())
            .containsExactlyEntriesOf(Map.of("document", "Document to gather links for."));
    }

    @Test
    void isImpactedBy()
    {
        var query = createQuery(null);
        assertThat(query.isImpactedBy(emptyChangelog(), null)).isFalse();
    }

    @Test
    void missingDocumentConfigurationReturnsError()
    {
        var query = createQuery(null);
        var definition = new QueryDefinitionStub(query, null);
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("""
            ### Error
            
            No document name specified""");

        var jsonOutput = formatterRegistry.format(result, JSON);
        assertThat(jsonOutput).isEqualTo("{\"error\":\"No document name specified\"}");
    }

    @Test
    void documentNotFoundReturnsEmptyResult()
    {
        var query = createQuery(null);
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "NonExistentDoc");
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("No results");

        var jsonOutput = formatterRegistry.format(result, JSON);
        assertThat(jsonOutput).isEqualTo("{\"items\":[]}");
    }

    @Test
    void collectsLinksFromDocumentSection()
    {
        vault.addDocumentInPath("Projects/Alpha", """
            # Project Alpha
            
            Introductory paragraph.
            
            ## Links
            
            - [Google](https://google.com)
            - [GitHub](https://github.com)
            
            ## Notes
            
            - [Ignored](https://ignored.com)
            """
        );

        var query = createQuery(null);
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "Alpha");
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("""
            
            | Name   | Url |
            | ------ | --- |
            | Google | https://google.com |
            | GitHub | https://github.com |
            
            """);

        var jsonOutput = formatterRegistry.format(result, JSON);
        assertThat(jsonOutput).isEqualTo(
            "{\"items\":[{\"name\":\"Google\",\"url\":\"https://google.com\"}," +
            "{\"name\":\"GitHub\",\"url\":\"https://github.com\"}]}");
    }

    @Test
    void matchesDocumentSectionWithEmojiTitle()
    {
        vault.addDocumentInPath("Projects/Beta", """
            # Project Beta
            
            ## 🔗 Links
            
            - [Alfred](https://www.alfredapp.com)
            """
        );

        var query = createQuery(null);
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "Beta");
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("""
            
            | Name   | Url |
            | ------ | --- |
            | Alfred | https://www.alfredapp.com |
            
            """);
    }

    @Test
    void matchesCustomDocumentSectionName()
    {
        vault.addDocumentInPath("Projects/Custom", """
            # Project Custom
            
            ## External
            
            - [CustomLink](https://custom.com)
            
            ## Links
            
            - [DefaultLink](https://default.com)
            """
        );

        var query = createQuery("External");
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "Custom");
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("""
            
            | Name       | Url |
            | ---------- | --- |
            | CustomLink | https://custom.com |
            
            """);
    }

    @Test
    void collectsLinksFromSubsectionsUnderLinksSection()
    {
        vault.addDocumentInPath("Projects/Gamma", """
            # Project Gamma
            
            ## Links
            
            - [Valid](https://valid.com)
            
            ### Sublinks
            
            - [Nested](https://nested.com)
            """
        );

        var query = createQuery(null);
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "Gamma");
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("""
            
            | Name   | Url |
            | ------ | --- |
            | Valid  | https://valid.com |
            | Nested | https://nested.com |
            
            """);
    }

    @Test
    void multipleLinksOnSameLineInDocument()
    {
        vault.addDocumentInPath("Projects/Delta", """
            # Project Delta
            
            ## Links
            
            See [First](https://first.com) as well as [Second](https://second.com) for details.
            """
        );

        var query = createQuery(null);
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "Delta");
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("""
            
            | Name   | Url |
            | ------ | --- |
            | First  | https://first.com |
            | Second | https://second.com |
            
            """);
    }

    @Test
    void collectsLinksFromJournalMarkers()
    {
        var stubJournal = new StubJournal(
            Map.of(
                "Zeta", Map.of(
                    "🔗", List.of(
                        new MarkedLine(LocalDate.of(2024, 1, 1),
                            "- [Alfred](https://www.alfredapp.com)"
                        ),
                        new MarkedLine(LocalDate.of(2024, 1, 2), "- [Docs](https://docs.alfredapp.com)")
                    )
                )
            ),
            Map.of("🔗", createMarker("🔗", true))
        );

        vault.addDocumentInPath("Projects/Zeta", "# Project Zeta");

        var query = new ExternalLinksQuery(
            vault,
            stubJournal,
            Optional.empty(),
            createQueryResultFactory()
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "Zeta");
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("""
            
            | Name   | Url |
            | ------ | --- |
            | Docs   | https://docs.alfredapp.com |
            | Alfred | https://www.alfredapp.com |
            
            """);
    }

    @Test
    void ordersLinksFromMultipleMarkersNewestToOldestOverall()
    {
        var stubJournal = new StubJournal(
            Map.of(
                "Multiple", Map.of(
                    "🔗", List.of(
                        new MarkedLine(LocalDate.of(2024, 1, 1), "- [Old Link](https://old.com)"),
                        new MarkedLine(LocalDate.of(2024, 1, 10), "- [Newest Link](https://newest.com)")
                    ),
                    "💡", List.of(
                        new MarkedLine(LocalDate.of(2024, 1, 5), "- [Middle Link](https://middle.com)")
                    )
                )
            ),
            Map.of(
                "🔗", createMarker("🔗", true),
                "💡", createMarker("💡", true)
            )
        );

        vault.addDocumentInPath("Projects/Multiple", "# Project Multiple");

        var query = new ExternalLinksQuery(
            vault,
            stubJournal,
            Optional.empty(),
            createQueryResultFactory()
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "Multiple");
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("""
            
            | Name        | Url |
            | ----------- | --- |
            | Newest Link | https://newest.com |
            | Middle Link | https://middle.com |
            | Old Link    | https://old.com |
            
            """);
    }

    @Test
    void ignoresNonConfiguredJournalMarkers()
    {
        var stubJournal = new StubJournal(
            Map.of(
                "Eta", Map.of(
                    "🔗", List.of(
                        new MarkedLine(LocalDate.of(2024, 1, 1), "- [Valid](https://valid.com)")
                    ),
                    "❗️", List.of(
                        new MarkedLine(LocalDate.of(2024, 1, 1), "- [Ignored](https://ignored.com)")
                    )
                )
            ),
            Map.of(
                "🔗", createMarker("🔗", true),
                "❗️", createMarker("❗️", false)
            )
        );

        vault.addDocumentInPath("Projects/Eta", "# Project Eta");

        var query = new ExternalLinksQuery(
            vault,
            stubJournal,
            Optional.empty(),
            createQueryResultFactory()
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "Eta");
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("""
            
            | Name  | Url |
            | ----- | --- |
            | Valid | https://valid.com |
            
            """);
    }

    @Test
    void noExportLinksMarkersDisablesJournalExtraction()
    {
        var stubJournal = new StubJournal(
            Map.of(
                "Theta", Map.of(
                    "🔗", List.of(
                        new MarkedLine(LocalDate.of(2024, 1, 1), "- [Valid](https://valid.com)")
                    )
                )
            ),
            Map.of("🔗", createMarker("🔗", false))
        );

        vault.addDocumentInPath("Projects/Theta", "# Project Theta");

        var query = new ExternalLinksQuery(
            vault,
            stubJournal,
            Optional.empty(),
            createQueryResultFactory()
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "Theta");
        var result = query.run(definition);
        assertThat(formatterRegistry.format(result, MARKDOWN)).isEqualTo("No results");
    }

    @Test
    void combinesDocumentAndJournalLinksInOrder()
    {
        var stubJournal = new StubJournal(
            Map.of(
                "Iota", Map.of(
                    "🔗", List.of(
                        new MarkedLine(LocalDate.of(2024, 1, 1),
                            "- [Journal Link](https://journal.example.com)"
                        )
                    )
                )
            ),
            Map.of("🔗", createMarker("🔗", true))
        );

        vault.addDocumentInPath("Projects/Iota", """
            # Project Iota
            
            ## Links
            
            - [Doc Link](https://doc.example.com)
            """
        );

        var query = new ExternalLinksQuery(
            vault,
            stubJournal,
            Optional.empty(),
            createQueryResultFactory()
        );
        var definition = new QueryDefinitionStub(query, null)
            .withConfiguration("document", "Iota");
        var result = query.run(definition);

        var markdownOutput = formatterRegistry.format(result, MARKDOWN);
        assertThat(markdownOutput).isEqualTo("""
            
            | Name         | Url |
            | ------------ | --- |
            | Doc Link     | https://doc.example.com |
            | Journal Link | https://journal.example.com |
            
            """);
    }

    private ExternalLinksQuery createQuery(String documentLinkSectionName)
    {
        return new ExternalLinksQuery(vault, journal, Optional.ofNullable(documentLinkSectionName),
            createQueryResultFactory()
        );
    }

    private static Marker createMarker(String name, boolean exportLinks)
    {
        var document = new VaultStub().addDocument(name, """
            ---
            export-links: %s
            ---
            """.formatted(exportLinks));
        return new Marker(document);
    }

    private static class StubJournal
        implements Journal
    {
        private final Map<String, Map<String, List<MarkedLine>>> documentMarkedLines;
        private final Map<String, Marker> markers;

        StubJournal(
            Map<String, Map<String, List<MarkedLine>>> documentMarkedLines,
            Map<String, Marker> markers)
        {
            this.documentMarkedLines = documentMarkedLines;
            this.markers = markers;
        }

        @Override
        public Map<String, Marker> markers()
        {
            return markers;
        }

        @Override
        public Map<String, List<MarkedLine>> markedLinesFor(
            String documentName, Set<String> markerNames)
        {
            var markedLines = documentMarkedLines.getOrDefault(documentName, emptyMap());
            var result = new HashMap<String, List<MarkedLine>>();
            for (String marker : markerNames)
            {
                if (markedLines.containsKey(marker))
                {
                    result.put(marker, markedLines.get(marker));
                }
            }
            return result;
        }

        @Override
        public Optional<Daily> latest()
        {
            return empty();
        }

        @Override
        public Optional<Daily> dailyBefore(Daily daily)
        {
            return empty();
        }

        @Override
        public Optional<Daily> dailyAfter(Daily daily)
        {
            return empty();
        }

        @Override
        public Stream<Daily> dailiesInPeriod(LocalDate start, LocalDate end)
        {
            return Stream.empty();
        }

        @Override
        public Set<String> referencedDocumentsIn(Collection<Daily> dailies)
        {
            return emptySet();
        }

        @Override
        public SortedMap<LocalDate, String> timelineFor(String documentName, int daysFromToday)
        {
            return emptySortedMap();
        }

        @Override
        public Stream<Daily> dailiesFor(String documentName)
        {
            return Stream.empty();
        }

        @Override
        public Optional<Daily> toDaily(Document dailyDocument)
        {
            return empty();
        }

        @Override
        public Optional<LocalDate> mostRecentMentionOf(String documentName)
        {
            return empty();
        }

        @Override
        public Optional<Weekly> weeklyBefore(Weekly weekly)
        {
            return empty();
        }

        @Override
        public Optional<Weekly> weeklyAfter(Weekly weekly)
        {
            return empty();
        }

        @Override
        public Optional<Weekly> weeklyFor(LocalDate date)
        {
            return empty();
        }

        @Override
        public Stream<Daily> dailiesForWeek(Weekly weekly)
        {
            return Stream.empty();
        }

        @Override
        public LocalDate firstDayOf(Weekly weekly)
        {
            return LocalDate.now();
        }

        @Override
        public Weekly computeWeeklyFor(LocalDate date)
        {
            return null;
        }

        @Override
        public int dayOfWeekNumberFor(LocalDate date)
        {
            return 0;
        }

        @Override
        public Dictionary markerSettings(String markerName)
        {
            return null;
        }

        @Override
        public boolean isMarkerDocument(Document document)
        {
            return false;
        }

        @Override
        public Map<String, List<MarkedLine>> markedLinesFor(
            String documentName, Set<String> markerNames, boolean removeMarkers)
        {
            return markedLinesFor(documentName, markerNames);
        }

        @Override
        public Map<String, List<MarkedLine>> markedLinesFor(
            String documentName, Set<String> markerNames, LocalDate date)
        {
            return markedLinesFor(documentName, markerNames);
        }
    }
}

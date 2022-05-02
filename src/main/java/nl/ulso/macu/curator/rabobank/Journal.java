package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.vault.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.groupingBy;
import static nl.ulso.macu.vault.Section.createAnchor;

/**
 * Builds a journal - what happened on a certain date - on top of the documents in a vault, for
 * all documents that support it. It's a fairly big structure to build, and it's reused across
 * multiple queries, which is why this is a singleton that updates its internal structure only
 * once per change in the vault.
 */
class Journal
{
    private static final String JOURNAL_FOLDER = "Journal";
    private static final String REVERSE_TIMELINE_SECTION = "Reverse timeline";
    private static final String UNCATEGORIZED_ACTIVITIES_SECTION = "Uncategorized activities";
    static final List<String> FOLDERS_IN_ORDER = List.of(
            JOURNAL_FOLDER, "GROW!", "Projects", "Teams", "Systems", "Contacts"
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(Journal.class);
    private final FileSystemVault vault;
    private long version;

    private final List<JournalEntry> entries;

    Journal(FileSystemVault vault)
    {
        this.vault = vault;
        this.version = -1;
        this.entries = new ArrayList<>();
    }

    Map<String, Map<Document, List<JournalEntry>>> forWeek(int year, int week)
    {
        refreshJournal();
        LOGGER.debug("Selecting entries for year {}, week {}", year, week);
        LocalDate day = LocalDate.of(year, 12, 31)
                .with(WEEK_OF_WEEK_BASED_YEAR, week)
                .with(previousOrSame(DayOfWeek.MONDAY));
        var weekDays = day.datesUntil(day.plusDays(7)).collect(Collectors.toSet());
        return entries.stream()
                .filter(entry -> weekDays.contains(entry.date()))
                .sorted(Comparator.comparing(JournalEntry::date))
                .collect(
                        groupingBy(JournalEntry::folder,
                                groupingBy(e -> e.section().document()))
                );
    }

    private void refreshJournal()
    {
        if (version == vault.version())
        {
            return;
        }
        LOGGER.debug("Rebuilding the journal");
        entries.clear();
        var finder = new JournalEntryFinder();
        vault.accept(finder);
        version = vault.version();
    }

    private class JournalEntryFinder
            extends BreadthFirstVaultVisitor
    {
        private static final Set<String> FOLDERS = new HashSet<>(FOLDERS_IN_ORDER);

        private static final Pattern TITLE_PATTERN =
                compile("^\\[\\[(\\d{4}-\\d{2}-\\d{2})]]: (.*)$");
        private String folder = null;

        @Override
        public void visit(Folder folder)
        {
            if (this.folder == null && FOLDERS.contains(folder.name()))
            {
                this.folder = folder.name();
            }
            if (this.folder != null)
            {
                super.visit(folder);
                this.folder = null;
            }
        }

        @Override
        public void visit(Document document)
        {
            if (folder != null)
            {
                super.visit(document);
            }
        }

        @Override
        public void visit(Section section)
        {
            if (!folder.contentEquals(JOURNAL_FOLDER))
            {
                if (section.level() == 2 &&
                        section.title().contentEquals(REVERSE_TIMELINE_SECTION))
                {
                    super.visit(section);
                }
                if (section.level() == 3)
                {
                    var matcher = TITLE_PATTERN.matcher(section.title());
                    if (matcher.matches())
                    {
                        var date = parseDate(matcher.group(1));
                        var subject = createAnchor(matcher.group(2));
                        entries.add(new JournalEntry(date, folder, section, subject));
                    }
                }
            }
            else
            {
                if (section.level() == 2 &&
                        section.title().contentEquals(UNCATEGORIZED_ACTIVITIES_SECTION))
                {
                    super.visit(section);
                }
                if (section.level() == 3)
                {
                    var date = parseDate(section.document().name());
                    var subject = createAnchor(section.title());
                    entries.add(new JournalEntry(date, folder, section, subject));
                }
            }
        }

        private LocalDate parseDate(String dateString)
        {
            try
            {
                return LocalDate.parse(dateString);
            }
            catch (DateTimeParseException e)
            {
                LOGGER.warn("Invalid date: {}. Falling back to default", dateString);
                return LocalDate.of(1976, 11, 30);
            }
        }
    }
}

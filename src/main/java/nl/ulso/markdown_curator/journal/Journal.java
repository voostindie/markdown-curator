package nl.ulso.markdown_curator.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.DataModelTemplate;
import nl.ulso.markdown_curator.vault.Document;
import nl.ulso.markdown_curator.vault.Vault;
import nl.ulso.markdown_curator.vault.event.*;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Stream;

import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class Journal
        extends DataModelTemplate
{
    private static final Logger LOGGER = getLogger(Journal.class);

    private final Vault vault;
    private final JournalSettings settings;
    private final NavigableMap<LocalDate, Daily> dailies;
    private final NavigableSet<Weekly> weeklies;

    @Inject
    Journal(Vault vault, JournalSettings settings)
    {
        this.vault = vault;
        this.settings = settings;
        this.dailies = new TreeMap<>();
        this.weeklies = new TreeSet<>();
    }

    @Override
    protected void fullRefresh()
    {
        var builder = new JournalBuilder(settings);
        vault.accept(builder);
        dailies.clear();
        weeklies.clear();
        builder.dailies().forEach(daily -> dailies.put(daily.date(), daily));
        weeklies.addAll(builder.weeklies());
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Built a journal for {} days and {} weeks",
                    dailies.size(), weeklies.size());
        }
    }

    @Override
    public void process(DocumentAdded event)
    {
        processDocumentUpdate(event.document());
    }

    @Override
    public void process(DocumentChanged event)
    {
        processDocumentUpdate(event.document());
    }

    private void processDocumentUpdate(Document document)
    {
        if (isJournalEntry(document))
        {
            var builder = new JournalBuilder(settings);
            document.accept(builder);
            builder.dailies().forEach(daily -> dailies.put(daily.date(), daily));
            weeklies.addAll(builder.weeklies());
            LOGGER.debug("Updated the journal for {}", document.name());
        }
    }

    @Override
    public void process(DocumentRemoved event)
    {
        var document = event.document();
        if (isJournalEntry(document))
        {
            var weekly = JournalBuilder.parseWeeklyFrom(document);
            weekly.ifPresentOrElse((w) -> {
                weeklies.remove(w);
                LOGGER.debug("Removed weekly {} from the journal", w);
            }, () ->
            {
                var date = JournalBuilder.parseDateFrom(document);
                date.ifPresent(d -> {
                    dailies.remove(d);
                    LOGGER.debug("Removed date {} from the journal", d);
                });
            });
        }
    }

    private boolean isJournalEntry(Document document)
    {
        var folder = document.folder();
        while (folder != vault)
        {
            if (folder.name().contentEquals(settings.journalFolderName()))
            {
                return true;
            }
            folder = folder.parent();
        }
        return false;
    }

    public SortedMap<LocalDate, String> timelineFor(String documentName)
    {
        var timeline = new TreeMap<LocalDate, String>(reverseOrder());
        dailiesFor(documentName)
                .forEach(daily -> timeline.put(daily.date(), daily.summaryFor(documentName)));
        return timeline;
    }

    public Stream<Daily> entriesUntilIncluding(LocalDate start, LocalDate end)
    {
        return start.datesUntil(end.plusDays(1))
                .map(dailies::get)
                .filter(Objects::nonNull);
    }

    public Set<String> referencedDocumentsIn(Collection<Daily> entries)
    {
        return entries.stream()
                .flatMap(daily -> daily.referencedDocuments().stream())
                .collect(toUnmodifiableSet());
    }

    public Optional<LocalDate> mostRecentMentionOf(String documentName)
    {
        return dailiesFor(documentName)
                .map(Daily::date)
                .max(naturalOrder());
    }

    public Optional<LocalDate> dailyBefore(LocalDate date)
    {
        return Optional.ofNullable(dailies.lowerKey(date));
    }

    public Optional<LocalDate> dailyAfter(LocalDate date)
    {
        return Optional.ofNullable(dailies.higherKey(date));
    }

    public Optional<Weekly> weeklyBefore(Weekly weekly)
    {
        return Optional.ofNullable(weeklies.lower(weekly));
    }

    public Optional<Weekly> weeklyAfter(Weekly weekly)
    {
        return Optional.ofNullable(weeklies.higher(weekly));
    }

    public Optional<Weekly> weeklyFor(LocalDate date)
    {
        var year = date.get(settings.weekFields().weekBasedYear());
        var week = date.get(settings.weekFields().weekOfWeekBasedYear());
        var weekly = new Weekly(year, week);
        if (weeklies.contains(weekly))
        {
            return Optional.of(weekly);
        }
        return Optional.empty();
    }

    private Stream<Daily> dailiesFor(String documentName)
    {
        return dailies.values().stream()
                .filter(entry -> entry.refersTo(documentName));
    }

    public Stream<Daily> dailiesForWeek(Weekly weekly)
    {
        var weekFields = settings.weekFields();
        var firstDayOfWeek = LocalDate.now()
                .with(weekFields.weekBasedYear(), weekly.year())
                .with(weekFields.weekOfWeekBasedYear(), weekly.week())
                .with(weekFields.dayOfWeek(), weekFields.getFirstDayOfWeek().getValue());
        return firstDayOfWeek.datesUntil(firstDayOfWeek.plusDays(7))
                .map(dailies::get)
                .filter(Objects::nonNull);
    }

    public int dayOfWeekNumberFor(LocalDate date)
    {
        return date.get(settings.weekFields().dayOfWeek());
    }

    public Vault vault()
    {
        return vault;
    }
}

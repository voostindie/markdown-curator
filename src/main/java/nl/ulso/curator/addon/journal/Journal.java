package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;
import nl.ulso.curator.vault.*;
import nl.ulso.dictionary.Dictionary;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static nl.ulso.curator.addon.journal.JournalBuilder.parseDateFrom;
import static nl.ulso.curator.addon.journal.JournalBuilder.parseWeeklyFrom;
import static nl.ulso.curator.change.Change.Kind.DELETE;
import static nl.ulso.curator.change.Change.isCreate;
import static nl.ulso.curator.change.Change.isDelete;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;
import static nl.ulso.date.LocalDates.parseDateOrNull;
import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class Journal
    extends ChangeProcessorTemplate
{
    private static final Logger LOGGER = getLogger(Journal.class);

    private final Vault vault;
    private final JournalSettings settings;
    private final NavigableMap<LocalDate, Daily> dailies;
    private final NavigableSet<Weekly> weeklies;
    private final Map<String, Marker> markers;

    @Inject
    Journal(Vault vault, JournalSettings settings)
    {
        this.vault = vault;
        this.settings = settings;
        this.dailies = new TreeMap<>();
        this.weeklies = new TreeSet<>();
        this.markers = new HashMap<>();
    }

    @Override
    protected Set<? extends ChangeHandler> createChangeHandlers()
    {
        return Set.of(
            newChangeHandler(isDailyEntry(), this::handleDailyUpdate),
            newChangeHandler(isWeeklyEntry(), this::handleWeeklyUpdate),
            newChangeHandler(isMarkerEntry(), this::handleMarkerUpdate)
        );
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(Daily.class, Weekly.class, Marker.class);
    }

    @Override
    protected boolean isResetRequired(Changelog changelog)
    {
        return super.isResetRequired(changelog) ||
               changelog.changes().anyMatch(isJournalFolder().and(isDelete().or(isCreate())));
    }

    private Predicate<Change<?>> isJournalFolder()
    {
        return isPayloadType(Folder.class).and((Change<?> change) ->
        {
            var folder = (Folder) change.value();
            return vault.folder(settings.journalFolderName())
                .map(root -> isInHierarchyOf(vault, root, folder))
                .orElse(false);
        });
    }

    private Predicate<Change<?>> isJournalEntry()
    {
        return isPayloadType(Document.class).and((Change<?> change) ->
        {
            var document = (Document) change.value();
            return vault.folder(settings.journalFolderName())
                .map(root -> isInHierarchyOf(vault, root, document.folder()))
                .orElse(false);
        });
    }

    private boolean isInHierarchyOf(Vault vault, Folder parent, Folder child)
    {
        var folder = child;
        while (folder != parent)
        {
            if (folder == vault)
            {
                return false;
            }
            folder = folder.parent();
        }
        return true;
    }

    private Predicate<Change<?>> isDailyEntry()
    {
        return isJournalEntry().and((Change<?> change) ->
            parseDateFrom((Document) change.value()).isPresent());
    }

    private Predicate<Change<?>> isWeeklyEntry()
    {
        return isJournalEntry().and((Change<?> change) ->
            parseWeeklyFrom((Document) change.value()).isPresent());
    }

    private Predicate<Change<?>> isMarkerEntry()
    {
        return isJournalEntry().and((Change<?> change) ->
        {
            var document = (Document) change.value();
            return document.folder().name().contentEquals(settings.markerSubFolderName())
                   && document.folder().parent().name()
                       .contentEquals(settings.journalFolderName());
        });
    }

    @Override
    public void reset(ChangeCollector collector)
    {
        dailies.clear();
        weeklies.clear();
        markers.clear();
        var builder = new JournalBuilder(settings);
        vault.accept(builder);
        builder.dailies().forEach(daily ->
        {
            dailies.put(daily.date(), daily);
            collector.create(daily, Daily.class);
        });
        builder.weeklies().forEach(weekly ->
        {
            weeklies.add(weekly);
            collector.create(weekly, Weekly.class);
        });
        weeklies.addAll(builder.weeklies());
        vault.folder(settings.journalFolderName())
            .flatMap(journalFolder -> journalFolder.folder(settings.markerSubFolderName()))
            .ifPresent(markerFolder -> markerFolder.documents()
                .forEach(document ->
                {
                    var marker = new Marker(document);
                    markers.put(document.name(), marker);
                    collector.create(marker, Marker.class);
                }));
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Built a journal for {} days, {} weeks and {} known markers.",
                dailies.size(), weeklies.size(), markers.size()
            );
        }
    }

    private void handleDailyUpdate(Change<?> change, ChangeCollector collector)
    {
        var document = (Document) change.value();
        var date = parseDateOrNull(document.name());
        if (date == null)
        {
            throw new IllegalStateException(
                "Cannot parse date from document name: " + document.name());
        }
        if (change.kind() == DELETE)
        {
            var daily = dailies.remove(date);
            LOGGER.debug("Removed daily '{}' from the journal.", date);
            collector.delete(daily, Daily.class);
        }
        else
        {
            var builder = new JournalBuilder(settings);
            document.accept(builder);
            var daily = builder.dailies().iterator().next();
            var previous = dailies.put(daily.date(), daily);
            LOGGER.debug("Updated the journal for daily '{}'.", document.name());
            if (previous == null)
            {
                collector.create(daily, Daily.class);
            }
            else
            {
                collector.update(previous, daily, Daily.class);
            }
        }
    }

    private void handleWeeklyUpdate(Change<?> change, ChangeCollector collector)
    {
        var document = (Document) change.value();
        parseWeeklyFrom(document).ifPresent(weekly ->
        {
            if (change.kind() == DELETE)
            {
                weeklies.remove(weekly);
                LOGGER.debug("Removed weekly '{}' from the journal.", weekly);
                collector.delete(weekly, Weekly.class);
            }
            else
            {
                LOGGER.debug("Updated the journal for weekly '{}'", document.name());
                if (weeklies.add(weekly))
                {
                    collector.create(weekly, Weekly.class);
                }
                else
                {
                    collector.update(weekly, Weekly.class);
                }
            }
        });
    }

    private void handleMarkerUpdate(Change<?> change, ChangeCollector collector)
    {
        var document = (Document) change.value();
        if (change.kind() == DELETE)
        {
            var marker = markers.remove(document.name());
            collector.delete(marker, Marker.class);
        }
        else
        {
            var marker = new Marker(document);
            markers.put(document.name(), marker);
            collector.create(marker, Marker.class);
        }
    }

    public Optional<Daily> toDaily(Document dailyDocument)
    {
        return parseDateFrom(dailyDocument).map(dailies::get);
    }

    public boolean isMarkerDocument(Document document)
    {
        return document.folder().name().contentEquals(settings.markerSubFolderName())
               && document.folder().parent().name().contentEquals(settings.journalFolderName());
    }

    public SortedMap<LocalDate, String> timelineFor(String documentName)
    {
        var timeline = new TreeMap<LocalDate, String>(reverseOrder());
        dailiesFor(documentName).forEach(
            daily -> timeline.put(daily.date(), daily.summaryFor(documentName)));
        return timeline;
    }

    public Map<String, List<MarkedLine>> markedLinesFor(
        String documentName, Set<String> markerNames)
    {
        return markedLinesFor(documentName, markerNames, true);
    }

    public Map<String, List<MarkedLine>> markedLinesFor(
        String documentName, Set<String> markerNames, boolean removeMarkers)
    {
        return dailiesFor(documentName).flatMap(
                daily -> daily.markedLinesFor(documentName, markerNames, removeMarkers).entrySet()
                    .stream())
            .collect(toMap(Entry::getKey, Entry::getValue, Journal::mergeLists, TreeMap::new));
    }

    public Map<String, List<MarkedLine>> markedLinesFor(
        String documentName, Set<String> markerNames, LocalDate date)
    {
        var daily = dailies.get(date);
        if (daily == null)
        {
            return emptyMap();
        }
        if (!daily.refersTo(documentName))
        {
            return emptyMap();
        }
        return daily.markedLinesFor(documentName, markerNames, true);
    }

    private static <T> List<T> mergeLists(List<T> first, List<T> second)
    {
        first.addAll(second);
        return first;
    }

    public Stream<Daily> entriesUntilIncluding(LocalDate start, LocalDate end)
    {
        return start.datesUntil(end.plusDays(1)).map(dailies::get).filter(Objects::nonNull);
    }

    public Optional<Daily> latest()
    {
        var latest = dailies.lastEntry();
        if (latest == null)
        {
            return Optional.empty();
        }
        return Optional.of(latest.getValue());
    }

    public Set<String> referencedDocumentsIn(Collection<Daily> entries)
    {
        return entries.stream().flatMap(daily -> daily.referencedDocuments().stream())
            .collect(toUnmodifiableSet());
    }

    public Optional<LocalDate> mostRecentMentionOf(String documentName)
    {
        return dailiesFor(documentName).map(Daily::date).max(naturalOrder());
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

    public Weekly computeWeeklyFor(LocalDate date)
    {
        var year = date.get(settings.weekFields().weekBasedYear());
        var week = date.get(settings.weekFields().weekOfWeekBasedYear());
        return new Weekly(year, week);
    }

    public Optional<Weekly> weeklyFor(LocalDate date)
    {
        var weekly = computeWeeklyFor(date);
        if (weeklies.contains(weekly))
        {
            return Optional.of(weekly);
        }
        return Optional.empty();
    }

    private Stream<Daily> dailiesFor(String documentName)
    {
        return dailies.values().stream().filter(entry -> entry.refersTo(documentName));
    }

    public Stream<Daily> dailiesForWeek(Weekly weekly)
    {
        var weekFields = settings.weekFields();
        var firstDayOfWeek = LocalDate.now().with(weekFields.weekBasedYear(), weekly.year())
            .with(weekFields.weekOfWeekBasedYear(), weekly.week())
            .with(weekFields.dayOfWeek(), weekFields.getFirstDayOfWeek().getValue());
        return firstDayOfWeek.datesUntil(firstDayOfWeek.plusDays(7)).map(dailies::get)
            .filter(Objects::nonNull);
    }

    public int dayOfWeekNumberFor(LocalDate date)
    {
        return date.get(settings.weekFields().dayOfWeek());
    }

    public Map<String, Marker> markers()
    {
        return unmodifiableMap(markers);
    }

    public Dictionary markerSettings(String markerName)
    {
        var marker = markers.get(markerName);
        return marker != null ? marker.settings() : emptyDictionary();
    }

    public Vault vault()
    {
        return vault;
    }
}

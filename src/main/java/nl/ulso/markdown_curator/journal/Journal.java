package nl.ulso.markdown_curator.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.*;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.Dictionary;
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
import static nl.ulso.markdown_curator.Change.*;
import static nl.ulso.markdown_curator.Change.Kind.DELETE;
import static nl.ulso.markdown_curator.journal.JournalBuilder.parseDateFrom;
import static nl.ulso.markdown_curator.journal.JournalBuilder.parseWeeklyFrom;
import static nl.ulso.markdown_curator.vault.Dictionary.emptyDictionary;
import static nl.ulso.markdown_curator.vault.LocalDates.parseDateOrNull;
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
        this.registerChangeHandler(isDailyEntry(), this::handleDailyUpdate);
        this.registerChangeHandler(isWeeklyEntry(), this::handleWeeklyUpdate);
        this.registerChangeHandler(isMarkerEntry(), this::handleMarkerUpdate);
    }

    @Override
    public Set<Class<?>> producedObjectTypes()
    {
        return Set.of(Daily.class, Weekly.class, Marker.class);
    }

    @Override
    protected boolean isFullRefreshRequired(Changelog changelog)
    {
        return super.isFullRefreshRequired(changelog) ||
               changelog.changes().anyMatch(isJournalFolder().and(isDelete().or(isCreate())));
    }

    private Predicate<Change<?>> isJournalFolder()
    {
        return isObjectType(Folder.class).and((Change<?> change) ->
        {
            var folder = (Folder) change.object();
            return vault.folder(settings.journalFolderName())
                .map(root -> isInHierarchyOf(vault, root, folder))
                .orElse(false);
        });
    }

    private Predicate<Change<?>> isJournalEntry()
    {
        return isObjectType(Document.class).and((Change<?> change) ->
        {
            var document = (Document) change.object();
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
            parseDateFrom((Document) change.object()).isPresent());
    }

    private Predicate<Change<?>> isWeeklyEntry()
    {
        return isJournalEntry().and((Change<?> change) ->
            parseWeeklyFrom((Document) change.object()).isPresent());
    }

    private Predicate<Change<?>> isMarkerEntry()
    {
        return isJournalEntry().and((Change<?> change) ->
        {
            var document = (Document) change.object();
            return document.folder().name().contentEquals(settings.markerSubFolderName())
                   && document.folder().parent().name()
                       .contentEquals(settings.journalFolderName());
        });
    }

    @Override
    public Collection<Change<?>> fullRefresh()
    {
        dailies.clear();
        weeklies.clear();
        markers.clear();
        var builder = new JournalBuilder(settings);
        vault.accept(builder);
        var changes = createChangeCollection();
        builder.dailies().forEach(daily ->
        {
            dailies.put(daily.date(), daily);
            changes.add(create(daily, Daily.class));
        });
        builder.weeklies().forEach(weekly ->
        {
            weeklies.add(weekly);
            changes.add(create(weekly, Weekly.class));
        });
        weeklies.addAll(builder.weeklies());
        vault.folder(settings.journalFolderName())
            .flatMap(journalFolder -> journalFolder.folder(settings.markerSubFolderName()))
            .ifPresent(markerFolder -> markerFolder.documents()
                .forEach(document ->
                {
                    var marker = new Marker(document);
                    markers.put(document.name(), marker);
                    changes.add(create(marker, Marker.class));
                }));
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Built a journal for {} days, {} weeks and {} known markers.",
                dailies.size(), weeklies.size(), markers.size()
            );
        }
        return changes;
    }

    private Collection<Change<?>> handleDailyUpdate(Change<?> change)
    {
        var document = (Document) change.object();
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
            return List.of(delete(daily, Daily.class));
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
                return List.of(create(daily, Daily.class));
            }
            else
            {
                return List.of(update(previous, daily, Daily.class));
            }
        }
    }

    private Collection<Change<?>> handleWeeklyUpdate(Change<?> change)
    {
        var document = (Document) change.object();
        var newChange = parseWeeklyFrom(document).map(weekly ->
        {
            if (change.kind() == DELETE)
            {
                weeklies.remove(weekly);
                LOGGER.debug("Removed weekly '{}' from the journal.", weekly);
                return delete(weekly, Weekly.class);
            }
            else
            {
                LOGGER.debug("Updated the journal for weekly '{}'", document.name());
                if (weeklies.add(weekly))
                {
                    return create(weekly, Weekly.class);
                }
                else
                {
                    return update(weekly, Weekly.class);
                }
            }
        }).orElseThrow(() -> new IllegalStateException(
            "Cannot parse weekly from document name: " + document.name())
        );
        return List.of(newChange);
    }

    private Collection<Change<?>> handleMarkerUpdate(Change<?> change)
    {
        var document = (Document) change.object();
        if (change.kind() == DELETE)
        {
            var marker = markers.remove(document.name());
            return List.of(delete(marker, Marker.class));
        }
        else
        {
            var marker = new Marker(document);
            markers.put(document.name(), marker);
            return List.of(create(marker, Marker.class));
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

package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.MapBasedEntityRepository;
import nl.ulso.curator.vault.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static nl.ulso.curator.addon.journal.Daily.parseDateFrom;
import static nl.ulso.date.LocalDates.parseDateOrNull;

@Singleton
final class DefaultDailyRepository
    extends MapBasedEntityRepository<Document, LocalDate, Daily>
    implements DailyRepository
{
    private final String journalFolderName;
    private final String activitiesSectionName;

    @Inject
    DefaultDailyRepository(JournalSettings settings)
    {
        this.journalFolderName = settings.journalFolderName();
        this.activitiesSectionName = settings.activitiesSectionName();
    }

    @Override
    protected Class<Document> sourceEntityClass()
    {
        return Document.class;
    }

    @Override
    protected Class<Daily> targetEntityClass()
    {
        return Daily.class;
    }

    @Override
    protected boolean isEntity(Document document)
    {
        return document.isInPath(journalFolderName)
               && parseDateOrNull(document.name()) != null;
    }

    @Override
    protected LocalDate entityKeyFrom(Document document)
    {
        return requireNonNull(parseDateOrNull(document.name()));
    }

    @Override
    protected Daily createEntityFrom(LocalDate date, Document document)
    {
        var finder = new ActivitiesSectionFinder();
        document.accept(finder);
        var section = finder.section != null ? finder.section : Section.EMPTY_SECTION;
        return new Daily(date, section);
    }

    @Override
    protected Map<LocalDate, Daily> createMap()
    {
        return new TreeMap<>();
    }

    @Override
    public Optional<Daily> latest()
    {
        var latest = navigableMap().lastEntry();
        if (latest == null)
        {
            return Optional.empty();
        }
        return Optional.of(latest.getValue());
    }

    @Override
    public Optional<Daily> dailyBefore(Daily daily)
    {
        return Optional.ofNullable(navigableMap().lowerKey(daily.date()))
            .map(navigableMap()::get);
    }

    @Override
    public Optional<Daily> dailyAfter(Daily daily)
    {
        return Optional.ofNullable(navigableMap().higherKey(daily.date()))
            .map(navigableMap()::get);
    }

    @Override
    public Stream<Daily> dailiesInPeriod(LocalDate start, LocalDate end)
    {
        var dailies = map();
        return start.datesUntil(end.plusDays(1)).map(dailies::get).filter(Objects::nonNull);
    }

    @Override
    public Set<String> referencedDocumentsIn(Collection<Daily> dailies)
    {
        return dailies.stream().flatMap(daily -> daily.referencedDocuments().stream())
            .collect(toUnmodifiableSet());
    }

    @Override
    public SortedMap<LocalDate, String> timelineFor(String documentName)
    {
        var timeline = new TreeMap<LocalDate, String>(reverseOrder());
        dailiesFor(documentName).forEach(
            daily -> timeline.put(daily.date(), daily.summaryFor(documentName)));
        return timeline;
    }

    @Override
    public Optional<Daily> dailyFor(LocalDate date)
    {
        return Optional.ofNullable(map().get(date));
    }

    @Override
    public Stream<Daily> dailiesFor(String documentName)
    {
        return entities().stream().filter(entry -> entry.refersTo(documentName));
    }

    @Override
    public Optional<Daily> toDaily(Document dailyDocument)
    {
        return parseDateFrom(dailyDocument).map(map()::get);
    }

    @Override
    public Optional<LocalDate> mostRecentMentionOf(String documentName)
    {
        return dailiesFor(documentName).map(Daily::date).max(naturalOrder());
    }

    @Override
    public String toString()
    {
        return DailyRepository.class.getSimpleName();
    }

    private class ActivitiesSectionFinder
        extends BreadthFirstVaultVisitor
    {
        private Section section;

        @Override
        public void visit(Section section)
        {
            if (this.section != null)
            {
                return;
            }
            if (section.level() == 2 &&
                section.sortableTitle().contentEquals(activitiesSectionName))
            {
                this.section = section;
            }
        }
    }
}

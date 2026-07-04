package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.MapBasedEntityRepository;
import nl.ulso.curator.vault.Document;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static nl.ulso.curator.addon.journal.Daily.parseDateFrom;

@Singleton
final class DefaultDailyRepository
    extends MapBasedEntityRepository<LocalDate, Daily>
    implements DailyRepository
{
    @Inject
    DefaultDailyRepository()
    {
    }

    @Override
    protected Class<Daily> entityClass()
    {
        return Daily.class;
    }

    @Override
    protected Class<?> repositoryClass()
    {
        return DailyRepository.class;
    }

    @Override
    protected LocalDate entityKeyFrom(Daily daily)
    {
        return daily.date();
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
        var daily = latest();
        while (daily.isPresent())
        {
            if (daily.get().refersTo(documentName))
            {
                return daily.map(Daily::date);
            }
            daily = dailyBefore(daily.get());
        }
        return Optional.empty();
    }
}

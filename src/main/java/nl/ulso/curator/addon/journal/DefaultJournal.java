package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@Singleton
final class DefaultJournal
    implements Journal
{
    private final DailyRepository dailyRepository;
    private final WeeklyRepository weeklyRepository;
    private final MarkerRepository markerRepository;

    @Inject
    DefaultJournal(
        DailyRepository dailyRepository,
        WeeklyRepository weeklyRepository,
        MarkerRepository markerRepository)
    {
        this.dailyRepository = dailyRepository;
        this.weeklyRepository = weeklyRepository;
        this.markerRepository = markerRepository;
    }

    @Override
    public Optional<Daily> latest()
    {
        return dailyRepository.latest();
    }

    @Override
    public Optional<Daily> dailyBefore(Daily daily)
    {
        return dailyRepository.dailyBefore(daily);
    }

    @Override
    public Optional<Daily> dailyAfter(Daily daily)
    {
        return dailyRepository.dailyAfter(daily);
    }

    @Override
    public Stream<Daily> dailiesInPeriod(LocalDate start, LocalDate end)
    {
        return dailyRepository.dailiesInPeriod(start, end);
    }

    @Override
    public Set<String> referencedDocumentsIn(Collection<Daily> dailies)
    {
        return dailyRepository.referencedDocumentsIn(dailies);
    }

    @Override
    public SortedMap<LocalDate, String> timelineFor(String documentName)
    {
        return dailyRepository.timelineFor(documentName);
    }

    @Override
    public Stream<Daily> dailiesFor(String documentName)
    {
        return dailyRepository.dailiesFor(documentName);
    }

    @Override
    public Optional<Daily> toDaily(Document dailyDocument)
    {
        return dailyRepository.toDaily(dailyDocument);
    }

    @Override
    public Optional<LocalDate> mostRecentMentionOf(String documentName)
    {
        return dailyRepository.mostRecentMentionOf(documentName);
    }

    @Override
    public Optional<Weekly> weeklyBefore(Weekly weekly)
    {
        return weeklyRepository.weeklyBefore(weekly);
    }

    @Override
    public Optional<Weekly> weeklyAfter(Weekly weekly)
    {
        return weeklyRepository.weeklyAfter(weekly);
    }

    @Override
    public Optional<Weekly> weeklyFor(LocalDate date)
    {
        return weeklyRepository.weeklyFor(date);
    }

    @Override
    public Stream<Daily> dailiesForWeek(Weekly weekly)
    {
        var firstDayOfWeek = firstDayOf(weekly);
        var lastDayOfWeek = firstDayOfWeek.plusDays(7);
        return firstDayOfWeek.datesUntil(lastDayOfWeek)
            .map(dailyRepository::dailyFor)
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    @Override
    public LocalDate firstDayOf(Weekly weekly)
    {
        return weeklyRepository.firstDayOf(weekly);
    }

    @Override
    public Weekly computeWeeklyFor(LocalDate date)
    {
        return weeklyRepository.computeWeeklyFor(date);
    }

    @Override
    public int dayOfWeekNumberFor(LocalDate date)
    {
        return weeklyRepository.dayOfWeekNumberFor(date);
    }

    @Override
    public Map<String, Marker> markers()
    {
        return markerRepository.markers();
    }

    @Override
    public Map<String, List<MarkedLine>> markedLinesFor(
        String documentName,
        Set<String> markerNames)
    {
        return markedLinesFor(documentName, markerNames, true);
    }

    @Override
    public Map<String, List<MarkedLine>> markedLinesFor(
        String documentName,
        Set<String> markerNames,
        boolean removeMarkers)
    {
        return dailyRepository.dailiesFor(documentName).flatMap(
                daily -> daily.markedLinesFor(documentName, markerNames, removeMarkers).entrySet()
                    .stream())
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, DefaultJournal::mergeLists,
                TreeMap::new
            ));
    }

    @Override
    public Map<String, List<MarkedLine>> markedLinesFor(
        String documentName,
        Set<String> markerNames,
        LocalDate date)
    {
        var emptyMap = Collections.<String, List<MarkedLine>>emptyMap();
        return dailyRepository.dailyFor(date).map(daily ->
        {
            if (!daily.refersTo(documentName))
            {
                return emptyMap;
            }
            return daily.markedLinesFor(documentName, markerNames, true);

        }).orElse(emptyMap);
    }

    @Override
    public Dictionary markerSettings(String markerName)
    {
        return markerRepository.markerSettings(markerName);
    }

    @Override
    public boolean isMarkerDocument(Document document)
    {
        return markerRepository.isMarkerDocument(document);
    }

    private static <T> List<T> mergeLists(List<T> first, List<T> second)
    {
        first.addAll(second);
        return first;
    }
}

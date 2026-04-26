package nl.ulso.curator.addon.journal;

import nl.ulso.curator.vault.Document;
import nl.ulso.dictionary.Dictionary;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

/// Overarching interface that gives access to journal capabilities. This is the only public
/// interface in the module.
public interface Journal
{
    /// @return the latest daily entry in the journal, if any.
    Optional<Daily> latest();

    Optional<Daily> dailyBefore(Daily daily);

    Optional<Daily> dailyAfter(Daily daily);

    Stream<Daily> dailiesInPeriod(LocalDate start, LocalDate end);

    Set<String> referencedDocumentsIn(Collection<Daily> dailies);

    SortedMap<LocalDate, String> timelineFor(String documentName);

    Stream<Daily> dailiesFor(String documentName);

    Optional<Daily> toDaily(Document dailyDocument);

    Optional<LocalDate> mostRecentMentionOf(String documentName);

    Optional<Weekly> weeklyBefore(Weekly weekly);

    Optional<Weekly> weeklyAfter(Weekly weekly);

    Optional<Weekly> weeklyFor(LocalDate date);

    Stream<Daily> dailiesForWeek(Weekly weekly);

    LocalDate firstDayOf(Weekly weekly);

    Weekly computeWeeklyFor(LocalDate date);

    int dayOfWeekNumberFor(LocalDate date);

    Map<String, Marker> markers();

    Dictionary markerSettings(String markerName);

    boolean isMarkerDocument(Document document);

    Map<String, List<MarkedLine>> markedLinesFor(
        String documentName, Set<String> markerNames);

    Map<String, List<MarkedLine>> markedLinesFor(
        String documentName, Set<String> markerNames, boolean removeMarkers);

    Map<String, List<MarkedLine>> markedLinesFor(
        String documentName, Set<String> markerNames, LocalDate date);
}

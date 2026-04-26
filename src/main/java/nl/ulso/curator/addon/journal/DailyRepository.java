package nl.ulso.curator.addon.journal;

import nl.ulso.curator.vault.Document;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

/// Keeps track of daily journal entries.
interface DailyRepository
{
    Optional<Daily> latest();

    Optional<Daily> dailyBefore(Daily daily);

    Optional<Daily> dailyAfter(Daily daily);

    Stream<Daily> dailiesInPeriod(LocalDate start, LocalDate end);

    Set<String> referencedDocumentsIn(Collection<Daily> dailies);

    SortedMap<LocalDate, String> timelineFor(String documentName);

    Optional<Daily> dailyFor(LocalDate date);

    Stream<Daily> dailiesFor(String documentName);

    Optional<Daily> toDaily(Document dailyDocument);

    Optional<LocalDate> mostRecentMentionOf(String documentName);
}

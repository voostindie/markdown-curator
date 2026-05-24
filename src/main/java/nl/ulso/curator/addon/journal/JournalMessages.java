package nl.ulso.curator.addon.journal;

import java.time.LocalDate;

interface JournalMessages
{
    String journalDay(LocalDate date);

    String journalWeek(int year, int week);

    String journalNext();

    String journalPrevious();

    String journalUp();

    String journalWeekDay(int day);

    String journalLatest();
}

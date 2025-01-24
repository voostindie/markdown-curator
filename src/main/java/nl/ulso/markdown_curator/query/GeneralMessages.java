package nl.ulso.markdown_curator.query;

import java.time.LocalDate;

public interface GeneralMessages
{
    String noResults();

    String journalDay(LocalDate date);

    String journalWeek(int year, int week);

    String journalNext();

    String journalPrevious();

    String journalUp();

    String journalWeekDay(int day);

    String journalLatest();
}

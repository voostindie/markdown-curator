package nl.ulso.curator.query;

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

    String projectLead();

    String projectPriority();

    String projectName();

    String projectLastModified();

    String projectStatus();

    String projectPriorityUnknown();

    String projectDateUnknown();

    String projectLeadUnknown();

    String projectStatusUnknown();
}

package nl.ulso.curator.query;

import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static java.lang.Character.toTitleCase;
import static java.util.ResourceBundle.getBundle;

final class ResourceBundledGeneralMessages
        implements GeneralMessages
{
    private final ResourceBundle bundle;
    private final DateTimeFormatter dayFormatter;

    public ResourceBundledGeneralMessages()
    {
        this(Optional.empty());
    }

    public ResourceBundledGeneralMessages(Locale locale)
    {
        this(Optional.of(locale));
    }

    @Inject
    ResourceBundledGeneralMessages(Optional<Locale> optionalLocale)
    {
        var locale = optionalLocale.orElse(Locale.ENGLISH);
        this.bundle = getBundle("GeneralMessages", locale);
        this.dayFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale);
    }

    @Override
    public String noResults()
    {
        return bundle.getString("results.none");
    }

    @Override
    public String journalDay(LocalDate date)
    {
        // MessageFormat doesn't support LocalDate. And it never will.
        // Converting to a legacy Date is not the right approach, so instead:
        var day = dayFormatter.format(date);
        return toTitleCase(day.charAt(0)) + day.substring(1);
    }

    @Override
    public String journalWeek(int year, int week)
    {
        return MessageFormat.format(bundle.getString("journal.week"),
                Integer.toString(year), Integer.toString(week));
    }

    @Override
    public String journalNext()
    {
        return bundle.getString("journal.next");
    }

    @Override
    public String journalPrevious()
    {
        return bundle.getString("journal.previous");
    }

    @Override
    public String journalUp()
    {
        return bundle.getString("journal.up");
    }

    @Override
    public String journalWeekDay(int day)
    {
        if (day < 1 || day > 7)
        {
            throw new IllegalStateException("Day must be in range [1, 7]");
        }
        return bundle.getString("journal.weekday." + day);
    }

    @Override
    public String journalLatest()
    {
        return bundle.getString("journal.latest");
    }

    @Override
    public String projectLead()
    {
        return bundle.getString("project.lead");
    }

    @Override
    public String projectPriority()
    {
        return bundle.getString("project.priority");
    }

    @Override
    public String projectName()
    {
        return bundle.getString("project.name");
    }

    @Override
    public String projectLastModified()
    {
        return bundle.getString("project.lastModified");
    }

    @Override
    public String projectStatus()
    {
        return bundle.getString("project.status");
    }

    @Override
    public String projectPriorityUnknown()
    {
        return bundle.getString("project.priority.unknown");
    }

    @Override
    public String projectDateUnknown()
    {
        return bundle.getString("project.lead.unknown");
    }

    @Override
    public String projectLeadUnknown()
    {
        return bundle.getString("project.lead.unknown");
    }

    @Override
    public String projectStatusUnknown()
    {
        return bundle.getString("project.status.unknown");
    }
}

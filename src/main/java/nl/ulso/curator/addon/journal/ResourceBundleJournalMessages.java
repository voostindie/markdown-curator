package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static java.lang.Character.toTitleCase;
import static java.util.ResourceBundle.getBundle;

@Singleton
final class ResourceBundleJournalMessages
    implements JournalMessages
{
    private final ResourceBundle bundle;
    private final DateTimeFormatter dayFormatter;

    ResourceBundleJournalMessages()
    {
        this(Optional.empty());
    }

    ResourceBundleJournalMessages(Locale locale)
    {
        this(Optional.of(locale));
    }

    @Inject
    ResourceBundleJournalMessages(Optional<Locale> optionalLocale)
    {
        var locale = optionalLocale.orElse(Locale.ENGLISH);
        this.bundle = getBundle("JournalMessages", locale);
        this.dayFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale);

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
}

package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import nl.ulso.curator.extension.ExtensionScope;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static java.lang.Character.toTitleCase;
import static java.util.ResourceBundle.Control.FORMAT_PROPERTIES;
import static java.util.ResourceBundle.Control.getNoFallbackControl;
import static java.util.ResourceBundle.getBundle;

@ExtensionScope
final class ResourceBundleJournalMessages
    implements JournalMessages
{
    private static final String BUNDLE_NAME = "JournalMessages";

    private final Provider<Locale> locale;

    ResourceBundleJournalMessages()
    {
        this(Locale.US);
    }

    ResourceBundleJournalMessages(Locale locale)
    {
        this(() -> locale);
    }

    /// The locale can change at runtime, so it's looked up whenever a message is needed.
    @Inject
    ResourceBundleJournalMessages(Provider<Locale> locale)
    {
        this.locale = locale;
    }

    /// Looks up the bundle for the current locale. Bundles are cached by the JVM. They don't fall
    /// back to the default locale of the JVM; if there is no bundle for the locale, the base
    /// bundle, in English, applies.
    private ResourceBundle bundle()
    {
        return getBundle(BUNDLE_NAME, locale.get(), getNoFallbackControl(FORMAT_PROPERTIES));
    }

    @Override
    public String journalDay(LocalDate date)
    {
        // MessageFormat doesn't support LocalDate. And it never will.
        // Converting to a legacy Date is not the right approach, so instead:
        var day = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(locale.get())
            .format(date);
        return toTitleCase(day.charAt(0)) + day.substring(1);
    }

    @Override
    public String journalWeek(int year, int week)
    {
        return MessageFormat.format(bundle().getString("journal.week"),
            Integer.toString(year), Integer.toString(week));
    }

    @Override
    public String journalNext()
    {
        return bundle().getString("journal.next");
    }

    @Override
    public String journalPrevious()
    {
        return bundle().getString("journal.previous");
    }

    @Override
    public String journalUp()
    {
        return bundle().getString("journal.up");
    }

    @Override
    public String journalWeekDay(int day)
    {
        if (day < 1 || day > 7)
        {
            throw new IllegalStateException("Day must be in range [1, 7]");
        }
        return bundle().getString("journal.weekday." + day);
    }

    @Override
    public String journalLatest()
    {
        return bundle().getString("journal.latest");
    }
}

package nl.ulso.date;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

/// Utility class for handling [LocalDate]s a little easier.
///
/// Unchecked exceptions are all the rage these days, meaning that your programs will crash horribly
/// if you try to parse user input into a [LocalDate]. There's no compiler hint anymore that
/// tells you to guard against it. Anyhow, this utility class addresses that.
public final class LocalDates
{
    private LocalDates()
    {
    }

    public static LocalDate parseDate(String dateString, Supplier<LocalDate> defaultDateSupplier)
    {
        try
        {
            return LocalDate.parse(dateString);
        }
        catch (DateTimeParseException _)
        {
            return defaultDateSupplier.get();
        }
    }

    public static LocalDate parseDateOrNull(String dateString)
    {
        return parseDate(dateString, () -> null);
    }
}

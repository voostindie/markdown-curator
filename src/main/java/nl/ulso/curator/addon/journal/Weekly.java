package nl.ulso.curator.addon.journal;

import nl.ulso.curator.vault.Document;

import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.Integer.compare;
import static java.lang.Integer.parseInt;
import static java.util.regex.Pattern.compile;

/// Represents a week in a year.
///
/// The name of a weekly in the vault MUST be formatted like "yyyy Week ww". The document name
/// pattern and the toString method are together in this class to ensure symmetry.
public record Weekly(int year, int week)
        implements Comparable<Weekly>
{
    private static final Pattern DOCUMENT_NAME_PATTERN = compile("^(\\d{4}) Week (\\d{2})$");

    @Override
    public int compareTo(Weekly o)
    {
        var c = compare(year, o.year);
        return c != 0 ? c : compare(week, o.week);
    }

    @Override
    public String toString()
    {
        return String.format("%s Week %02d", year, week);
    }

    public static boolean isWeekly(Document document)
    {
        return DOCUMENT_NAME_PATTERN.matcher(document.name()).matches();
    }

    public static Optional<Weekly> parseWeeklyFrom(Document document)
    {
        var matcher = DOCUMENT_NAME_PATTERN.matcher(document.name());
        if (matcher.matches())
        {
            var year = parseInt(matcher.group(1));
            var week = parseInt(matcher.group(2));
            return Optional.of(new Weekly(year, week));
        }
        return Optional.empty();
    }
}

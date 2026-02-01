package nl.ulso.curator.addon.journal;

import java.util.regex.Pattern;

import static java.lang.Integer.compare;
import static java.util.regex.Pattern.compile;

/// Represents a week in a year.
///
/// The name of a weekly in the vault MUST be formatted like "yyyy Week ww". The document name
/// pattern and the toString method are together in this class to ensure symmetry.
public record Weekly(int year, int week)
        implements Comparable<Weekly>
{
    static final Pattern DOCUMENT_NAME_PATTERN = compile("^(\\d{4}) Week (\\d{2})$");

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
}

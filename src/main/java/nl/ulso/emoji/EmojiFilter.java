package nl.ulso.emoji;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;
import static java.util.regex.Pattern.compile;

/**
 * Utility class for cleaning up strings with emojis.
 */
public class EmojiFilter
{
    private static final Pattern FILTER =
            compile("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", UNICODE_CHARACTER_CLASS);

    private EmojiFilter()
    {

    }

    public static String stripEmojis(String text)
    {
        return FILTER.matcher(text).replaceAll("");
    }
}

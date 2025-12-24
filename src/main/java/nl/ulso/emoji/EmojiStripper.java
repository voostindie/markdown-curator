package nl.ulso.emoji;

import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;
import static java.util.regex.Pattern.compile;

/// Utility class for cleaning up strings with emojis.
///
/// All this utility does is remove emojis from a string. It does not trim whitespace for example!
///
/// The implementation uses a regular expression not to detect and strip out emoji characters,
/// but to define what are non-emoji characters; that's easier. Then the pattern is negated, and
/// everything that matches it ("is not a non-emoji") is replaced with an empty string.
public final class EmojiStripper
    implements Function<String, String>
{
    // See <https://www.baeldung.com/java-string-remove-emojis>, option 3.
    private static final Pattern FILTER =
            compile("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", UNICODE_CHARACTER_CLASS);

    @Override
    public String apply(String text)
    {
        return stripEmojisFrom(text);
    }

    public static String stripEmojisFrom(String text)
    {
        return FILTER.matcher(text).replaceAll("");
    }
}

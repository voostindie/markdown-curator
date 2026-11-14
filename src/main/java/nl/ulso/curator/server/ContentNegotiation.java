package nl.ulso.curator.server;

import nl.ulso.curator.query.OutputFormat;

import java.util.*;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingDouble;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.joining;

/// The output formats a client asks for, and the logic to select the best one from the formats that
/// are actually available.
///
/// A client can ask for an output format in two ways:
///
/// 1. Explicitly, by name, through the `_format` query parameter. This is the only way to select
///    between formats that share a MIME type. If the format isn't available, nothing is selected.
/// 2. Through the `Accept` header, which is a list of media ranges, possibly with wildcards and
///    quality values, e.g. `text/csv, application/json;q=0.5, */*;q=0.1`.
///
/// Without either, the default format is selected.
///
/// Because the formats that are available can change at runtime - extensions come and go - the
/// selection is made per request, against the formats available at that moment.
public final class ContentNegotiation
{
    /// Name of the query parameter that selects an output format by name.
    public static final String FORMAT_PARAMETER = "_format";

    private static final ContentNegotiation NONE = new ContentNegotiation(null, List.of());

    private final String formatName;
    private final List<MediaRange> mediaRanges;

    private ContentNegotiation(String formatName, List<MediaRange> mediaRanges)
    {
        this.formatName = formatName;
        this.mediaRanges = mediaRanges;
    }

    /// @param acceptHeader the value of the `Accept` header; may be `null`.
    /// @param formatName   the explicitly requested format name; may be `null`.
    public static ContentNegotiation of(String acceptHeader, String formatName)
    {
        var name = formatName == null || formatName.isBlank()
                   ? null
                   : formatName.strip().toLowerCase(Locale.ROOT);
        var ranges = MediaRange.parse(acceptHeader);
        if (name == null && ranges.isEmpty())
        {
            return NONE;
        }
        return new ContentNegotiation(name, ranges);
    }

    /// @return a negotiation that has no preferences; it always selects the default format.
    public static ContentNegotiation none()
    {
        return NONE;
    }

    /// @return a negotiation that explicitly asks for the given format.
    public static ContentNegotiation of(OutputFormat outputFormat)
    {
        return of(null, outputFormat.name());
    }

    /// Selects the best output format from the available formats.
    ///
    /// - If a format was requested by name, that format is selected, if it is available.
    /// - If there are no preferences at all, the default format is selected.
    /// - Otherwise, the format with the highest quality value in the `Accept` header is selected.
    ///   The quality of a format is determined by the most specific media range that matches its
    ///   MIME type. Ties are broken by specificity, then by the order of the media ranges in the
    ///   header, then in favor of the default format, and finally by name.
    ///
    /// @return the selected format, or nothing if none of the requested formats is available; the
    /// server should respond with "406 Not Acceptable" in that case.
    public Optional<OutputFormat> select(
        Collection<OutputFormat> availableFormats,
        OutputFormat defaultFormat)
    {
        if (formatName != null)
        {
            return availableFormats.stream()
                .filter(format -> format.name().equals(formatName))
                .findFirst();
        }
        if (mediaRanges.isEmpty())
        {
            return availableFormats.stream()
                .filter(format -> format.name().equals(defaultFormat.name()))
                .findFirst()
                .or(() -> availableFormats.stream().min(comparing(OutputFormat::name)));
        }
        return availableFormats.stream()
            .flatMap(format -> match(format, defaultFormat).stream())
            .min(comparingDouble(Match::quality).reversed()
                .thenComparing(comparingInt(Match::specificity).reversed())
                .thenComparingInt(Match::position)
                .thenComparing(match -> !match.isDefault())
                .thenComparing(match -> match.format().name()))
            .map(Match::format);
    }

    /// @return a human-readable description of the requested formats, and the available formats.
    public String describeFailure(Collection<OutputFormat> availableFormats)
    {
        var requested = formatName != null
                        ? "'" + formatName + "'"
                        : mediaRanges.stream().map(MediaRange::toString).collect(joining(", "));
        var available = availableFormats.stream()
            .sorted(comparing(OutputFormat::name))
            .map(format -> format.name() + " (" + format.mimeType() + ")")
            .collect(joining(", "));
        return "None of the requested output formats is available: " + requested + ". " +
               "Available formats: " + available + ".";
    }

    /// @return the best match for the format, if any media range matches it with a quality above 0.
    private Optional<Match> match(OutputFormat format, OutputFormat defaultFormat)
    {
        var slash = format.mimeType().indexOf('/');
        var type = format.mimeType().substring(0, slash);
        var subtype = format.mimeType().substring(slash + 1);
        return mediaRanges.stream()
            .filter(range -> range.matches(type, subtype))
            .min(comparingInt(MediaRange::specificity).reversed()
                .thenComparingInt(MediaRange::position))
            .filter(range -> range.quality() > 0)
            .map(range -> new Match(format, range.quality(), range.specificity(),
                range.position(), format.name().equals(defaultFormat.name())));
    }

    @Override
    public String toString()
    {
        return formatName != null ? "format=" + formatName : "accept=" + mediaRanges;
    }

    private record Match(
        OutputFormat format, double quality, int specificity, int position, boolean isDefault)
    {
    }

    /// A single media range from an `Accept` header.
    ///
    /// @param specificity 0 for `*/*`, 1 for `type/*`, 2 for `type/subtype`.
    record MediaRange(String type, String subtype, double quality, int specificity, int position)
    {
        static List<MediaRange> parse(String header)
        {
            if (header == null || header.isBlank())
            {
                return List.of();
            }
            var ranges = new ArrayList<MediaRange>();
            for (var element : splitOutsideQuotes(header, ','))
            {
                parseRange(element, ranges.size()).ifPresent(ranges::add);
            }
            return List.copyOf(ranges);
        }

        private static Optional<MediaRange> parseRange(String element, int position)
        {
            var parts = splitOutsideQuotes(element, ';');
            var mediaType = parts[0].strip().toLowerCase(Locale.ROOT);
            var slash = mediaType.indexOf('/');
            if (slash < 1 || slash == mediaType.length() - 1)
            {
                return Optional.empty();
            }
            var type = mediaType.substring(0, slash).strip();
            var subtype = mediaType.substring(slash + 1).strip();
            if (type.equals("*") && !subtype.equals("*"))
            {
                return Optional.empty();
            }
            return parseQuality(parts).map(quality -> new MediaRange(
                type, subtype, quality, specificityOf(type, subtype), position));
        }

        /// @return the quality value of the media range, or nothing if it is invalid.
        private static Optional<Double> parseQuality(String[] parts)
        {
            var quality = 1.0;
            for (var i = 1; i < parts.length; i++)
            {
                var parameter = parts[i].strip();
                var equals = parameter.indexOf('=');
                if (equals > 0 && parameter.substring(0, equals).strip().equalsIgnoreCase("q"))
                {
                    try
                    {
                        quality = Double.parseDouble(parameter.substring(equals + 1).strip());
                    }
                    catch (NumberFormatException _)
                    {
                        return Optional.empty();
                    }
                    if (Double.isNaN(quality) || quality < 0 || quality > 1)
                    {
                        return Optional.empty();
                    }
                }
            }
            return Optional.of(quality);
        }

        private static int specificityOf(String type, String subtype)
        {
            if (type.equals("*"))
            {
                return 0;
            }
            return subtype.equals("*") ? 1 : 2;
        }

        /// Splits the value on the separator, except where the separator is inside a quoted string;
        /// parameter values in an `Accept` header may be quoted, and may contain separators.
        private static String[] splitOutsideQuotes(String value, char separator)
        {
            var parts = new ArrayList<String>();
            var start = 0;
            var quoted = false;
            var escaped = false;
            for (var i = 0; i < value.length(); i++)
            {
                var c = value.charAt(i);
                if (escaped)
                {
                    escaped = false;
                }
                else if (quoted && c == '\\')
                {
                    escaped = true;
                }
                else if (c == '"')
                {
                    quoted = !quoted;
                }
                else if (!quoted && c == separator)
                {
                    parts.add(value.substring(start, i));
                    start = i + 1;
                }
            }
            parts.add(value.substring(start));
            return parts.toArray(String[]::new);
        }

        boolean matches(String formatType, String formatSubtype)
        {
            return switch (specificity)
            {
                case 0 -> true;
                case 1 -> type.equals(formatType);
                default -> type.equals(formatType) && subtype.equals(formatSubtype);
            };
        }

        @Override
        public String toString()
        {
            return type + "/" + subtype + (quality < 1 ? ";q=" + quality : "");
        }
    }
}

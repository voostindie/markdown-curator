package nl.ulso.curator.query;

import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/// Format in which a [QueryResult] can be output.
///
/// Output formats are open-ended: next to the built-in [#MARKDOWN] and [#JSON] formats, extensions
/// can introduce their own, simply by providing [QueryResultFormatter]s for them. A format is
/// available as long as at least one formatter for it is registered.
///
/// A format is identified by its name, which is case-insensitive and therefore normalized to lower
/// case. Names must be unique: two formats with the same name must be equal in every respect.
/// Different formats may share a MIME type, however; clients of the HTTP server can then select a
/// format by name.
///
/// @param name     the name of the format, as used in the `_format` property of query blocks and
///                 the `_format` query parameter of the HTTP server; also the language of the
///                 fenced code block that embeds the output in a document, unless it's inline.
/// @param mimeType the MIME type of the format, for content negotiation by the HTTP server; a plain
///                 `type/subtype`, without wildcards or parameters.
/// @param inline   whether output in this format is embedded in a document as-is; if not, it's
///                 wrapped in a fenced code block.
public record OutputFormat(String name, String mimeType, boolean inline)
{
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9][a-z0-9._+-]*");
    private static final Pattern MIME_TYPE_PATTERN =
        Pattern.compile("[a-z0-9][a-z0-9!#$&^_.+-]*/[a-z0-9][a-z0-9!#$&^_.+-]*");

    public static final OutputFormat MARKDOWN = new OutputFormat("markdown", "text/markdown", true);
    public static final OutputFormat JSON = new OutputFormat("json", "application/json");

    public OutputFormat
    {
        requireNonNull(name, "name");
        requireNonNull(mimeType, "mimeType");
        name = name.strip().toLowerCase(Locale.ROOT);
        mimeType = mimeType.strip().toLowerCase(Locale.ROOT);
        if (!NAME_PATTERN.matcher(name).matches())
        {
            throw new IllegalArgumentException("Invalid output format name: '" + name + "'.");
        }
        if (!MIME_TYPE_PATTERN.matcher(mimeType).matches())
        {
            throw new IllegalArgumentException("Invalid MIME type: '" + mimeType + "'.");
        }
    }

    /// Creates an output format that is embedded in documents in a fenced code block.
    public OutputFormat(String name, String mimeType)
    {
        this(name, mimeType, false);
    }
}

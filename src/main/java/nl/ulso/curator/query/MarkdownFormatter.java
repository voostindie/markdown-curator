package nl.ulso.curator.query;

import static nl.ulso.curator.query.OutputFormat.MARKDOWN;

/// Base class for Markdown formatters, with convenience methods.
public abstract class MarkdownFormatter
{
    public final OutputFormat outputFormat()
    {
        return MARKDOWN;
    }
}

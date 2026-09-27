package nl.ulso.curator.query;

import static nl.ulso.curator.query.OutputFormat.JSON;

/// Base class for JSON formatters, with convenience methods.
public abstract class JsonFormatter
{
    public final OutputFormat outputFormat()
    {
        return JSON;
    }
}

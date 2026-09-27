package nl.ulso.curator.query;

public record OutputFormat(String name, String mimeType)
{
    public static final OutputFormat MARKDOWN = new OutputFormat("markdown", "text/markdown");
    public static final OutputFormat JSON = new OutputFormat("json", "application/json");
}

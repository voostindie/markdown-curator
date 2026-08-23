package nl.ulso.curator.query;

public record OutputFormat(String name, String mimeType)
{
    public static final OutputFormat MARKDOWN = new OutputFormat("markdown", "text/markdown");
}

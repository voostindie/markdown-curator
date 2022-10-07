package nl.ulso.markdown_curator.query;

public final class NoOpResult
        implements QueryResult
{
    NoOpResult()
    {
    }

    @Override
    public String toMarkdown()
    {
        throw new UnsupportedOperationException("Can't get Markdown from a NoOpResult!");
    }
}

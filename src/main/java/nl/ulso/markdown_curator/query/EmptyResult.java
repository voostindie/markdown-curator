package nl.ulso.markdown_curator.query;

final class EmptyResult
        implements QueryResult
{
    static final EmptyResult INSTANCE = new EmptyResult();

    private EmptyResult()
    {
    }

    @Override
    public String toMarkdown()
    {
        return "No results";
    }
}

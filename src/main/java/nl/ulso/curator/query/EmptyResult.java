package nl.ulso.curator.query;

final class EmptyResult
        implements QueryResult
{
    private final String message;

    EmptyResult(String message)
    {
        this.message = message;
    }

    @Override
    public String toMarkdown()
    {
        return message;
    }
}

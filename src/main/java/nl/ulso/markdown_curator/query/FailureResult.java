package nl.ulso.markdown_curator.query;

class FailureResult
        implements QueryResult
{
    private final String errorMessage;

    public FailureResult(String errorMessage)
    {
        this.errorMessage = errorMessage.trim();
    }

    @Override
    public String toMarkdown()
    {
        return errorMessage;
    }
}

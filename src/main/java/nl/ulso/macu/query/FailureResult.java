package nl.ulso.macu.query;

class FailureResult
        implements QueryResult
{
    private final String errorMessage;

    public FailureResult(String errorMessage)
    {
        this.errorMessage = errorMessage.trim();
    }

    @Override
    public boolean isSuccess()
    {
        return false;
    }

    @Override
    public String toMarkdown()
    {
        return errorMessage;
    }
}

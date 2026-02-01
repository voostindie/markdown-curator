package nl.ulso.curator.query;

import static java.lang.System.lineSeparator;

final class ErrorResult
        implements QueryResult
{
    private final String errorMessage;

    public ErrorResult(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toMarkdown()
    {
        return "### Error" + lineSeparator() + lineSeparator() + errorMessage;
    }
}

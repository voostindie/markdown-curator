package nl.ulso.markdown_curator.query;

import static java.lang.System.lineSeparator;

class ErrorResult
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

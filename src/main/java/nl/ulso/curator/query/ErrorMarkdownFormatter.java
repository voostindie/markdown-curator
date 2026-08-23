package nl.ulso.curator.query;

import jakarta.inject.Inject;

import static java.lang.System.lineSeparator;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;

public final class ErrorMarkdownFormatter
    implements QueryResultFormatter<ErrorResult>
{
    @Inject
    public ErrorMarkdownFormatter()
    {
    }

    @Override
    public Class<ErrorResult> queryResultType()
    {
        return ErrorResult.class;
    }

    @Override
    public OutputFormat outputFormat()
    {
        return MARKDOWN;
    }

    @Override
    public String format(ErrorResult result)
    {
        return "### Error" + lineSeparator() + lineSeparator() + result.errorMessage();
    }
}

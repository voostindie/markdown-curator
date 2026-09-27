package nl.ulso.curator.query;

import jakarta.inject.Inject;

import static java.lang.System.lineSeparator;

public final class ErrorMarkdownFormatter
    extends MarkdownFormatter
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
    public String format(ErrorResult result)
    {
        return "### Error" + lineSeparator() + lineSeparator() + result.errorMessage();
    }
}

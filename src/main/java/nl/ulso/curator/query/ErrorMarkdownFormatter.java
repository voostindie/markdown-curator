package nl.ulso.curator.query;

import jakarta.inject.Inject;

import static java.lang.System.lineSeparator;

final class ErrorMarkdownFormatter
    extends MarkdownFormatter
    implements QueryResultFormatter<ErrorResult>
{
    @Inject
    ErrorMarkdownFormatter()
    {
        // Default constructor for Dagger injection
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

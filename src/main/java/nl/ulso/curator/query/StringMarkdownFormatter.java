package nl.ulso.curator.query;

import jakarta.inject.Inject;

import static nl.ulso.curator.query.OutputFormat.MARKDOWN;

final class StringMarkdownFormatter
    implements QueryResultFormatter<StringResult>
{
    @Inject
    StringMarkdownFormatter()
    {
    }

    @Override
    public Class<StringResult> queryResultType()
    {
        return StringResult.class;
    }

    @Override
    public OutputFormat outputFormat()
    {
        return MARKDOWN;
    }

    @Override
    public String format(StringResult result)
    {
        return result.outputMessage();
    }
}

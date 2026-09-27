package nl.ulso.curator.query;

import jakarta.inject.Inject;

final class StringMarkdownFormatter
    extends MarkdownFormatter
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
    public String format(StringResult result)
    {
        return result.outputMessage();
    }
}

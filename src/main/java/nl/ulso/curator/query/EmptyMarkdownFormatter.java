package nl.ulso.curator.query;

import jakarta.inject.Inject;

final class EmptyMarkdownFormatter
    extends MarkdownFormatter
    implements QueryResultFormatter<EmptyResult>
{
    private final String message;

    @Inject
    EmptyMarkdownFormatter(GeneralMessages messages)
    {
        this.message = messages.noResults();
    }

    @Override
    public Class<EmptyResult> queryResultType()
    {
        return EmptyResult.class;
    }

    @Override
    public String format(EmptyResult result)
    {
        return message;
    }
}

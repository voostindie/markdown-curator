package nl.ulso.curator.query;

import jakarta.inject.Inject;

final class EmptyMarkdownFormatter
    extends MarkdownFormatter
    implements QueryResultFormatter<EmptyResult>
{
    private final GeneralMessages messages;

    @Inject
    EmptyMarkdownFormatter(GeneralMessages messages)
    {
        this.messages = messages;
    }

    @Override
    public Class<EmptyResult> queryResultType()
    {
        return EmptyResult.class;
    }

    @Override
    public String format(EmptyResult result)
    {
        // Not cached: the locale can change at runtime.
        return messages.noResults();
    }
}

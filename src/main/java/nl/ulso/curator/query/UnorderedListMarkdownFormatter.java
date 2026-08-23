package nl.ulso.curator.query;

import jakarta.inject.Inject;

import static java.lang.System.lineSeparator;
import static nl.ulso.curator.query.OutputFormat.MARKDOWN;

final class UnorderedListMarkdownFormatter
    implements QueryResultFormatter<UnorderedListResult>
{
    @Inject
    public UnorderedListMarkdownFormatter()
    {
    }

    @Override
    public Class<UnorderedListResult> queryResultType()
    {
        return UnorderedListResult.class;
    }

    @Override
    public OutputFormat outputFormat()
    {
        return MARKDOWN;
    }

    @Override
    public String format(UnorderedListResult result)
    {
        var builder = new StringBuilder();
        for (String row : result.rows())
        {
            builder.append("- ")
                .append(row)
                .append(lineSeparator());
        }
        return builder.toString();
    }
}

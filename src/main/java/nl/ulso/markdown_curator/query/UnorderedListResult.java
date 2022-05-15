package nl.ulso.markdown_curator.query;

import java.util.List;

import static java.lang.System.lineSeparator;
import static java.util.Collections.unmodifiableList;

class UnorderedListResult
        implements QueryResult
{
    private final List<String> rows;

    public UnorderedListResult(List<String> rows)
    {
        this.rows = unmodifiableList(rows);
    }

    @Override
    public String toMarkdown()
    {
        if (rows.isEmpty())
        {
            return "No results";
        }
        var builder = new StringBuilder();
        for (String row : rows)
        {
            builder.append("- ")
                    .append(row)
                    .append(lineSeparator());
        }
        return builder.toString();
    }
}

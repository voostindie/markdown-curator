package nl.ulso.markdown_curator.query;

import java.util.Collections;
import java.util.List;

import static java.lang.System.lineSeparator;

class ListResult
        implements QueryResult
{
    private final List<String> rows;

    public ListResult(List<String> rows)
    {
        this.rows = Collections.unmodifiableList(rows);
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
        return builder.toString().trim();
    }
}

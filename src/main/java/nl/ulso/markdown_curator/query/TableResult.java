package nl.ulso.markdown_curator.query;

import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.Collections.unmodifiableList;

public class TableResult
        implements QueryResult
{
    private final List<String> columns;
    private final List<Map<String, String>> rows;

    TableResult(List<String> columns, List<Map<String, String>> rows)
    {
        this.columns = unmodifiableList(columns);
        this.rows = unmodifiableList(rows);
    }

    @Override
    public String toMarkdown()
    {
        var widths = findColumnWidths();
        var width = columns.size();
        var builder = new StringBuilder();
        builder.append("|");
        for (var i = 0; i < width; i++)
        {
            builder.append(" ");
            var column = capitalize(columns.get(i));
            builder.append(column);
            builder.append(" ".repeat(Math.max(0, widths[i] - column.length())));
            builder.append(" |");
        }
        builder.append(lineSeparator());
        builder.append("|");
        for (var i = 0; i < width; i++)
        {
            builder.append(" ");
            builder.append("-".repeat(widths[i]));
            builder.append(" |");
        }
        builder.append(lineSeparator());
        for (Map<String, String> row : rows)
        {
            builder.append("|");
            for (var i = 0; i < width; i++)
            {
                builder.append(" ");
                var column = row.getOrDefault(columns.get(i), "");
                builder.append(column);
                builder.append(" ".repeat(Math.max(0, widths[i] - column.length())));
                builder.append(" |");
            }
            builder.append(lineSeparator());
        }
        return builder.toString();
    }

    private String capitalize(String string)
    {
        var length = string.length();
        if (length == 0)
        {
            return string;
        }
        if (length == 1)
        {
            return string.toUpperCase();
        }
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    private int[] findColumnWidths()
    {
        var width = columns.size();
        var widths = new int[width];
        for (var i = 0; i < width; i++)
        {
            widths[i] = columns.get(i).length();
        }
        for (Map<String, String> row : rows)
        {
            for (var i = 0; i < width - 1; i++)
            {
                widths[i] = Math.max(widths[i], row.getOrDefault(columns.get(i), "").length());
            }
        }
        return widths;
    }
}

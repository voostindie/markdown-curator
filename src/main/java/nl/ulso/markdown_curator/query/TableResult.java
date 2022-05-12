package nl.ulso.markdown_curator.query;

import java.util.*;

import static java.lang.System.lineSeparator;

class TableResult
        implements QueryResult
{
    private final List<String> columns;
    private final List<Map<String, String>> rows;

    public TableResult(List<String> columns, List<Map<String, String>> rows)
    {
        this.columns = Collections.unmodifiableList(columns);
        this.rows = Collections.unmodifiableList(rows);
    }

    @Override
    public String toMarkdown()
    {
        if (rows.isEmpty())
        {
            return "No results";
        }
        var widths = findColumnWidths();
        var width = columns.size();
        var builder = new StringBuilder();
        builder.append("|");
        for (var i = 0; i < width; i++)
        {
            builder.append(" ");
            builder.append(String.format("%-" + widths[i] + "s", capitalize(columns.get(i))));
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
                builder.append(String.format(" %-" + widths[i] + "s",
                        row.getOrDefault(columns.get(i), "")));
                builder.append(" |");
            }
            builder.append(lineSeparator());
        }
        return builder.toString().trim();
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
        return string.substring(0,1).toUpperCase() + string.substring(1);
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
            for (var i = 0; i < width; i++)
            {
                widths[i] = Math.max(widths[i], row.getOrDefault(columns.get(i), "").length());
            }
        }
        return widths;
    }
}

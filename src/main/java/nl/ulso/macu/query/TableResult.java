package nl.ulso.macu.query;

import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;

class TableResult
        implements QueryResult
{
    private final List<String> columns;
    private final List<Map<String, String>> rows;

    public TableResult(List<String> columns, List<Map<String, String>> rows)
    {
        this.columns = columns;
        this.rows = rows;
    }

    @Override
    public boolean isSuccess()
    {
        return true;
    }

    @Override
    public List<String> columns()
    {
        return columns;
    }

    @Override
    public List<Map<String, String>> rows()
    {
        return rows;
    }

    @Override
    public String errorMessage()
    {
        return "";
    }

    @Override
    public String toString()
    {
        if (rows.isEmpty())
        {
            return "No results";
        }
        var widths = findColumnWidths();
        var width = columns.size();
        var builder = new StringBuilder();
        builder.append('|');
        for (var i = 0; i < width; i++)
        {
            builder.append(String.format("%-" + widths[i] + "s", columns.get(i)));
            builder.append("|");
        }
        builder.append(lineSeparator());
        builder.append('|');
        for (var i = 0; i < width; i++)
        {
            builder.append(String.format("%-" + widths[i] + "s", '-').replace(' ', '-'));
            builder.append('|');
        }
        builder.append(lineSeparator());
        rows.forEach(row -> {
            builder.append('|');
            for (var i = 0; i < width; i++)
            {
                builder.append(String.format("%-" + widths[i] + "s",
                        row.getOrDefault(columns.get(i), "")));
                builder.append("|");
            }
            builder.append(lineSeparator());
        });
        return builder.toString().trim();
    }

    private int[] findColumnWidths()
    {
        var width = columns.size();
        var widths = new int[width];
        for (var i = 0; i < width; i++)
        {
            widths[i] = columns.get(i).length();
        }
        rows.forEach(row -> {
            for (var i = 0; i < width; i++)
            {
                widths[i] = Math.max(widths[i], row.getOrDefault(columns.get(i), "").length());
            }
        });
        return widths;
    }
}

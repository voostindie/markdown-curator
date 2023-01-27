package nl.ulso.markdown_curator.query;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.lang.System.lineSeparator;
import static java.util.Collections.unmodifiableList;
import static java.util.regex.Pattern.compile;

public class TableResult
        implements QueryResult
{
    private static final char NORMAL_HYPHEN = '-';
    private static final char NON_BREAKING_HYPHEN = '‑';
    private static final Predicate<String> DATE_PREDICATE =
            compile("^\\d{4}-\\d{2}-\\d{2}$").asPredicate();

    private final List<String> columns;
    private final List<Map<String, String>> rows;
    private final String summaryText;

    TableResult(List<String> columns, List<Map<String, String>> rows, String summaryText)
    {
        this.columns = unmodifiableList(columns);
        this.rows = unmodifiableList(rows);
        this.summaryText = summaryText;
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
                var column = applyObsidianFormattingWorkaround(
                        row.getOrDefault(columns.get(i), ""));
                builder.append(column);
                builder.append(" ".repeat(Math.max(0, widths[i] - column.length())));
                builder.append(" |");
            }
            builder.append(lineSeparator());
        }
        builder.append(lineSeparator());
        builder.append("(*")
                .append(summaryText)
                .append("*)")
                .append(lineSeparator());
        return builder.toString();
    }

    /*
     * The table formatting in Obsidian is such that it wraps along hyphens, which is correct most
     * of the time, except when the colum is a date. In that case the dates might be wrapped. To
     * prevent dates being wrapped this method replaces the normal hyphens with non-breaking
     * hyphens. They look exactly the same visually, which is great when looking at the table
     * in Markdown format.
     *
     * I'm aware that this workaround only applies if dates are formatted as "YYYY-MM-DD", which
     * is what I do. For people formatting dates differently, this workaround simply doesn't work.
     */
    private String applyObsidianFormattingWorkaround(String column)
    {
        if (DATE_PREDICATE.test(column))
        {
            return column.replace(NORMAL_HYPHEN, NON_BREAKING_HYPHEN);
        }
        return column;
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

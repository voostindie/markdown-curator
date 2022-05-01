package nl.ulso.macu.query;

import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;

class ListResult
        implements QueryResult
{
    private final List<String> rows;

    public ListResult(List<String> rows)
    {
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
        return List.of("Result");
    }

    @Override
    public List<Map<String, String>> rows()
    {
        return rows.stream().map(row -> Map.of("Result", row)).toList();
    }

    @Override
    public String errorMessage()
    {
        return "";
    }

    @Override
    public String toMarkdown()
    {
        if (rows.isEmpty())
        {
            return "No results";
        }
        var builder = new StringBuilder();
        rows.forEach(row ->
                builder.append("- ")
                        .append(row)
                        .append(lineSeparator()));
        return builder.toString().trim();
    }
}

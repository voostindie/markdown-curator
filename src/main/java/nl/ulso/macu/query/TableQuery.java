package nl.ulso.macu.query;

import nl.ulso.macu.vault.QueryBlock;
import nl.ulso.macu.vault.Vault;

import java.util.*;

import static java.util.Collections.reverse;
import static nl.ulso.macu.query.QueryResult.failure;

public class TableQuery
        implements Query
{
    private final Vault vault;

    public TableQuery(Vault vault)
    {
        this.vault = vault;
    }

    @Override
    public String name()
    {
        return "table";
    }

    @Override
    public String description()
    {
        return "generates a sorted table of pages, with optional front matter fields in " +
                "additional columns";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
                "folder", "folder to list pages from",
                "recurse", "whether to recurse into directories; defaults to false",
                "reverse", "whether to reverse the list; defaults to false",
                "columns", "list of front matter fields to create columns for",
                "sort", "column to sort on, defaults to the name of the page"
        );
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        var configuration = queryBlock.configuration();
        var folder = configuration.string("folder", null);
        if (folder == null)
        {
            return failure("Property 'folder' is missing.");
        }
        var recurse = configuration.bool("recurse", false);
        var reverse = configuration.bool("reverse", false);
        var sort = configuration.string("sort", "Name");
        var frontMatterColumns = configuration.listOfStrings("columns");
        var finder = new PageFinder(folder, recurse);
        vault.accept(finder);
        var table = new ArrayList<>(finder.pages().stream()
                .map(document -> {
                    Map<String, String> row = new HashMap<>();
                    row.put("Name", document.link());
                    var frontMatter = document.frontMatter();
                    for (String column : frontMatterColumns)
                    {
                        row.put(column, frontMatter.string(column, ""));
                    }
                    return row;
                })
                .sorted(Comparator.comparing(row -> row.get(sort)))
                .toList());
        if (reverse)
        {
            reverse(table);
        }
        List<String> columns = new ArrayList<>();
        columns.add("Name");
        columns.addAll(frontMatterColumns);
        var i = columns.indexOf(sort);
        if (i > 0)
        {
            columns.remove(i);
            columns.add(0, sort);
        }
        return QueryResult.table(columns, table);
    }
}

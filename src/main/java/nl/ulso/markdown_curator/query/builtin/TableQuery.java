package nl.ulso.markdown_curator.query.builtin;

import jakarta.inject.Inject;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.Vault;

import java.util.*;

import static java.util.Collections.reverse;

public final class TableQuery
    extends FolderQuery
    implements Query
{
    private final QueryResultFactory resultFactory;

    @Inject
    public TableQuery(Vault vault, QueryResultFactory resultFactory)
    {
        super(vault);
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return "table";
    }

    @Override
    public String description()
    {
        return "Generates a sorted table of pages, with optional front matter fields in " +
               "additional columns.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
            "folder", "folder to list pages from; defaults to this document's folder",
            "recurse", "whether to recurse into directories; defaults to false",
            "reverse", "whether to reverse the list; defaults to false",
            "columns", "list of front matter fields to create columns for",
            "sort", "column to sort on, defaults to the name of the page"
        );
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var configuration = definition.configuration();
        var folder = resolveFolderName(definition);
        var recurse = configuration.bool("recurse", false);
        var reverse = configuration.bool("reverse", false);
        var sort = configuration.string("sort", "Name");
        var frontMatterColumns = configuration.listOfStrings("columns");
        var finder = new PageFinder(folder, recurse);
        vault().accept(finder);
        var table = new ArrayList<>(finder.pages().stream()
            .map(document ->
            {
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
            columns.addFirst(sort);
        }
        return resultFactory.table(columns, table);
    }
}

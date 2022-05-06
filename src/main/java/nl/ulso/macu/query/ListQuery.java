package nl.ulso.macu.query;

import nl.ulso.macu.vault.*;

import java.util.*;

import static java.util.Collections.reverse;
import static nl.ulso.macu.query.QueryResult.failure;
import static nl.ulso.macu.query.QueryResult.list;

public class ListQuery
        implements Query
{
    private final Vault vault;

    public ListQuery(Vault vault)
    {
        this.vault = vault;
    }

    @Override
    public String name()
    {
        return "list";
    }

    @Override
    public String description()
    {
        return "generates a sorted list of pages";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
                "folder", "folder to list pages from",
                "recurse", "whether to recurse into directories; defaults to false",
                "reverse", "whether to reverse the list; defaults to false"
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
        var finder = new PageFinder(folder, recurse);
        vault.accept(finder);
        var list = new ArrayList<>(finder.pages().stream().map(Document::link).sorted().toList());
        if (reverse)
        {
            reverse(list);
        }
        return list(list);
    }

}

package nl.ulso.curator.query.builtin;

import jakarta.inject.Inject;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.Vault;

import java.util.ArrayList;
import java.util.Map;

import static java.util.Collections.reverse;
import static java.util.Comparator.comparing;

public final class ListQuery
    extends FolderQuery
    implements Query
{
    private final QueryResultFactory resultFactory;

    @Inject
    public ListQuery(Vault vault, QueryResultFactory resultFactory)
    {
        super(vault);
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return "list";
    }

    @Override
    public String description()
    {
        return "Generates a sorted list of pages in a folder.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
            "folder",
            "folder to list pages from; defaults to the folder of the current document",
            "recurse", "whether to recurse into directories; defaults to false",
            "reverse", "whether to reverse the list; defaults to false"
        );
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var configuration = definition.configuration();
        var folder = resolveFolderName(definition);
        var recurse = configuration.bool("recurse", false);
        var reverse = configuration.bool("reverse", false);
        var finder = new PageFinder(folder, recurse);
        vault().accept(finder);
        var list = new ArrayList<>(finder.pages().stream()
            .sorted(comparing(Document::sortableTitle))
            .map(Document::link)
            .toList());
        if (reverse)
        {
            reverse(list);
        }
        return resultFactory.unorderedList(list);
    }
}

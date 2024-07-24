package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.Document;
import nl.ulso.markdown_curator.vault.Vault;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Map;

import static java.util.Collections.reverse;

public final class ListQuery
        implements Query
{
    private final Vault vault;
    private final QueryResultFactory resultFactory;

    @Inject
    public ListQuery(Vault vault, QueryResultFactory resultFactory)
    {
        this.vault = vault;
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
        var documentFolder = definition.document().folder().name();
        var folder = configuration.string("folder", documentFolder);
        var recurse = configuration.bool("recurse", false);
        var reverse = configuration.bool("reverse", false);
        var finder = new PageFinder(folder, recurse);
        vault.accept(finder);
        var list = new ArrayList<>(finder.pages().stream().map(Document::link).sorted().toList());
        if (reverse)
        {
            reverse(list);
        }
        return resultFactory.unorderedList(list);
    }
}

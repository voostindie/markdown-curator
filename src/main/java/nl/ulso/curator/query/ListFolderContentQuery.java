package nl.ulso.curator.query;

import jakarta.inject.Inject;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.vault.*;

import java.util.Map;

final class ListFolderContentQuery
    implements Query
{
    private final Vault vault;
    private final DocumentPathResolver resolver;

    @Inject
    ListFolderContentQuery(Vault vault, DocumentPathResolver resolver)
    {
        this.vault = vault;
        this.resolver = resolver;
    }

    @Override
    public String name()
    {
        return "ls";
    }

    @Override
    public String description()
    {
        return "Lists folder content";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
            "path", "Path to folder to list, defaults to the vault root"
        );
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return false;
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var path = definition.configuration().string("path", "");
        var parts = path.split("/");
        Folder folder = vault;
        for (var part : parts)
        {
            if (part.isBlank())
            {
                continue;
            }
            var subfolder = folder.folder(part);
            if (subfolder.isEmpty())
            {
                return new ErrorResult("Invalid path: " + path + ".");
            }
            folder = subfolder.get();
        }
        return new FolderResult(folder, resolver);
    }
}

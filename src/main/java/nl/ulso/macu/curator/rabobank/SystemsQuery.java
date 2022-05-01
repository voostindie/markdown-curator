package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.*;

import java.util.*;

import static java.util.Collections.emptyMap;

class SystemsQuery
        implements Query
{
    private final Vault vault;

    SystemsQuery(Vault vault) {this.vault = vault;}

    @Override
    public String name()
    {
        return "systems";
    }

    @Override
    public String description()
    {
        return "outputs all systems in my scope in a table";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        return vault.folder("Systems").map(folder -> {
            var finder = new SystemFinder();
            folder.accept(finder);
            var systems = finder.systems;
            Collections.sort(systems);
            return QueryResult.list(systems);
        }).orElse(QueryResult.failure("Couldn't find the folder 'Systems'"));
    }

    private static class SystemFinder
            extends BreadthFirstVaultVisitor
    {
        private final List<String> systems = new ArrayList<>();

        @Override
        public void visit(Folder folder)
        {
            // Don't recurse into subfolders!
            if (folder.name().equals("Systems"))
            {
                super.visit(folder);
            }
        }

        @Override
        public void visit(Document document)
        {
            var domain = document.frontMatter().string("domain", null);
            if (domain != null)
            {
                systems.add(document.link());
            }
        }
    }
}

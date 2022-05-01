package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.*;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.regex.Pattern.compile;

class ProjectsQuery
        implements Query
{
    private final Vault vault;

    ProjectsQuery(Vault vault) {this.vault = vault;}

    @Override
    public String name()
    {
        return "projects";
    }

    @Override
    public String description()
    {
        return "outputs all active projects in a table";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        var finder = new ProjectFinder();
        vault.accept(finder);
        return QueryResult.table(List.of("Date", "Project"), finder.projects);
    }

    private static class ProjectFinder
            extends BreadthFirstVaultVisitor
    {
        private static final Pattern TITLE_PATTERN =
                compile("^\\[\\[(\\d{4}-\\d{2}-\\d{2})]]: (.*)$");

        private final List<Map<String, String>> projects = new ArrayList<>();

        @Override
        public void visit(Vault vault)
        {
            super.visit(vault);
            projects.sort(comparing((Map<String, String> e) -> e.get("Date")).reversed());
        }

        @Override
        public void visit(Folder folder)
        {
            if (folder.name().equals("Projects"))
            {
                super.visit(folder);
            }
        }

        @Override
        public void visit(Document document)
        {
            if (document.folder().name().equals("Projects"))
            {
                super.visit(document);
            }
        }

        @Override
        public void visit(Section section)
        {
            if (section.level() == 2 && section.title().contentEquals("Reverse timeline")
                    && section.fragments().size() > 1
                    && section.fragments().get(1) instanceof Section subsection)
            {
                var matcher = TITLE_PATTERN.matcher(subsection.title());
                if (matcher.matches())
                {
                    projects.add(Map.of(
                            "Project", "[[" + section.document().name() + "]]",
                            "Date", matcher.group(1)
                    ));
                }
            }
        }
    }
}

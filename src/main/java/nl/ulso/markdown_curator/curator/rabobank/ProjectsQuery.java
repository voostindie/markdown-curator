package nl.ulso.markdown_curator.curator.rabobank;

import nl.ulso.markdown_curator.query.Query;
import nl.ulso.markdown_curator.query.QueryResult;
import nl.ulso.markdown_curator.vault.*;

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
        return vault.folder("Projects").map(folder -> {
            var finder = new ProjectFinder();
            folder.accept(finder);
            var projects = finder.projects;
            projects.sort(comparing((Map<String, String> e) -> e.get("Date")).reversed());
            return QueryResult.table(List.of("Date", "Project"), projects);
        }).orElse(QueryResult.failure("Couldn't find the folder 'Projects'"));
    }

    private static class ProjectFinder
            extends BreadthFirstVaultVisitor
    {
        private static final Pattern TITLE_PATTERN =
                compile("^\\[\\[(\\d{4}-\\d{2}-\\d{2})]]: (.*)$");

        private final List<Map<String, String>> projects = new ArrayList<>();

        @Override
        public void visit(Folder folder)
        {
            // Don't recurse into subfolders!
            if (folder.name().equals("Projects"))
            {
                super.visit(folder);
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
                            "Project", section.document().link(),
                            "Date", matcher.group(1)
                    ));
                }
            }
        }
    }
}

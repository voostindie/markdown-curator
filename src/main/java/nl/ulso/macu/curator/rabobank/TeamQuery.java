package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.*;

import java.util.*;

import static java.util.Comparator.comparing;
import static nl.ulso.macu.query.QueryResult.failure;
import static nl.ulso.macu.query.QueryResult.table;

class TeamQuery
        implements Query
{
    private final Vault vault;

    TeamQuery(Vault vault) {this.vault = vault;}

    @Override
    public String name()
    {
        return "team";
    }

    @Override
    public String description()
    {
        return "outputs an overview of team members";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("name", "Name of the team, defaults to 'none'");
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        return vault.folder("Contacts").map(folder -> {
            var team = queryBlock.configuration().string("name", "none");
            var finder = new MemberFinder(team);
            folder.accept(finder);
            var members = finder.members;
            members.sort(comparing((Map<String, String> e) -> e.get("Name")));
            return table(List.of("Name", "Scope"), members);
        }).orElse(failure("Couldn't find the folder 'Contacts'"));
    }

    private static class MemberFinder
            extends BreadthFirstVaultVisitor
    {
        private final List<Map<String, String>> members = new ArrayList<>();
        private final String team;

        public MemberFinder(String team)
        {
            this.team = team;
        }

        @Override
        public void visit(Document document)
        {
            if (team.contentEquals(document.frontMatter().string("team", "")))
            {
                members.add(Map.of(
                        "Name", document.link(),
                        "Scope", document.frontMatter().string("scope", "Unknown")));
            }
        }
    }
}

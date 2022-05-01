package nl.ulso.macu.curator.rabobank;

import nl.ulso.macu.query.Query;
import nl.ulso.macu.query.QueryResult;
import nl.ulso.macu.vault.*;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.regex.Pattern.compile;
import static nl.ulso.macu.query.QueryResult.failure;
import static nl.ulso.macu.query.QueryResult.table;

class ArchitectureDecisionRecordsQuery
        implements Query
{
    private final Vault vault;

    ArchitectureDecisionRecordsQuery(Vault vault) {this.vault = vault;}

    @Override
    public String name()
    {
        return "adrs";
    }

    @Override
    public String description()
    {
        return "outputs an overview of all ADRs";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        return vault.folder("ADRs").map(folder -> {
            var finder = new AdrFinder();
            folder.accept(finder);
            var adrs = finder.adrs;
            adrs.sort(comparing((Map<String, String> e) -> e.get("ID")));
            return table(List.of("ID", "Date", "Status", "Name"), adrs);
        }).orElse(failure("Couldn't find the folder 'ADRs'"));
    }

    private static class AdrFinder
            extends BreadthFirstVaultVisitor
    {
        private static final Pattern CHANGES_PATTERN =
                compile("^- \\[\\[(\\d{4}-\\d{2}-\\d{2})]]: (.*)$");

        private final List<Map<String, String>> adrs = new ArrayList<>();

        private String date;
        private String status;

        @Override
        public void visit(Document document)
        {
            var id = document.frontMatter().string("id", null);
            if (id == null)
            {
                return;
            }
            date = "";
            status = "";
            super.visit(document);
            adrs.add(Map.of(
                    "ID", id,
                    "Date", date,
                    "Status", status,
                    "Name", document.link()
            ));
        }

        @Override
        public void visit(Section section)
        {
            if (section.level() == 2 && section.fragments().size() > 0
                    && section.fragments().get(0) instanceof TextBlock textBlock)
            {
                if (section.title().contentEquals("Changes"))
                {
                    var lines = new ArrayList<>(textBlock.lines());
                    if (lines.isEmpty())
                    {
                        return;
                    }
                    lines.sort(Comparator.reverseOrder());
                    var mostRecent = lines.get(0);
                    var matcher = CHANGES_PATTERN.matcher(mostRecent);
                    if (matcher.matches())
                    {
                        date = matcher.group(1);
                    }
                }
                else if (section.title().contentEquals("Status"))
                {
                    var lines = textBlock.lines();
                    if (lines.size() > 0)
                    {
                        status = lines.get(0);
                    }
                }
            }
        }
    }
}

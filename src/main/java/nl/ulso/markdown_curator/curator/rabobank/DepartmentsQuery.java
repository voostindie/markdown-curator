package nl.ulso.markdown_curator.curator.rabobank;

import nl.ulso.markdown_curator.query.Query;
import nl.ulso.markdown_curator.query.QueryResult;
import nl.ulso.markdown_curator.vault.Document;
import nl.ulso.markdown_curator.vault.QueryBlock;

import java.util.*;

class DepartmentsQuery
        implements Query
{
    private final OrgChart orgChart;

    public DepartmentsQuery(OrgChart orgChart)
    {
        this.orgChart = orgChart;
    }

    @Override
    public String name()
    {
        return "departments";
    }

    @Override
    public String description()
    {
        return "lists all departments of a team";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of("style", "list or table; defaults to list"
                , "roles", "names of the roles in the table");
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        var parent = queryBlock.document().name();
        var style = queryBlock.configuration().string("style", "list");
        switch (style)
        {
            case "list":
                var units = orgChart.forParent(parent).stream()
                        .map(OrgUnit::team)
                        .map(Document::link)
                        .sorted()
                        .toList();
                return QueryResult.list(units);
            case "table":
                var roles = queryBlock.configuration().listOfStrings("roles");
                var rows = orgChart.forParent(parent).stream()
                        .sorted(Comparator.comparing(orgUnit -> orgUnit.team().name()))
                        .map(orgUnit ->
                        {
                            Map<String, String> row = new HashMap<>();
                            row.put("Name", orgUnit.team().link());
                            for (String role : roles)
                            {
                                var contact = orgUnit.leadership().get(role);
                                if (contact != null)
                                {
                                    row.put(role, contact.link());
                                }
                            }
                            return row;
                        }).toList();
                var columns = new ArrayList<String>(roles.size() + 1);
                columns.add("Name");
                columns.addAll(roles);
                return QueryResult.table(columns, rows);
            default:
                return QueryResult.failure("Unsupported style: " + style);
        }
    }
}

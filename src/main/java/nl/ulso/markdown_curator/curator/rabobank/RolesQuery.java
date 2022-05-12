package nl.ulso.markdown_curator.curator.rabobank;

import nl.ulso.markdown_curator.query.Query;
import nl.ulso.markdown_curator.query.QueryResult;
import nl.ulso.markdown_curator.vault.QueryBlock;

import java.util.*;

import static java.util.Comparator.comparing;

class RolesQuery
        implements Query
{
    private final OrgChart orgChart;

    public RolesQuery(OrgChart orgChart)
    {
        this.orgChart = orgChart;
    }

    @Override
    public String name()
    {
        return "roles";
    }

    @Override
    public String description()
    {
        return "lists all roles of a contact";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Collections.emptyMap();
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        var contact = queryBlock.document().name();
        var roles = orgChart.forContact(contact).stream()
                .sorted(comparing(orgUnit -> orgUnit.team().name()))
                .map(unit -> Map.of("Team", unit.team().link(),
                        "Role", unit.leadership().entrySet().stream()
                                .filter(e -> e.getValue().name().contentEquals(contact))
                                .map(Map.Entry::getKey).findFirst().orElse("")))
                .toList();
        return QueryResult.table(List.of("Team", "Role"), roles);
    }
}

package nl.ulso.macu.query;

import nl.ulso.macu.vault.QueryBlock;

import java.util.Map;

import static java.lang.System.lineSeparator;
import static nl.ulso.macu.query.QueryResult.failure;

public class HelpQuery
        implements Query
{
    private final QueryCatalog catalog;

    public HelpQuery(QueryCatalog catalog)
    {
        this.catalog = catalog;
    }

    @Override
    public String name()
    {
        return "help";
    }

    @Override
    public String description()
    {
        return "shows detailed help information for a query";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
                "query", "Name of the query to get help for."
        );
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        var query = catalog.query(queryBlock.configuration().string("query", "help"));
        var builder = new StringBuilder();
        builder.append("**")
                .append(query.name())
                .append("**: ")
                .append(query.description())
                .append(".")
                .append(lineSeparator())
                .append(lineSeparator());
        Map<String, String> config = query.supportedConfiguration();
        if (config.isEmpty())
        {
            builder.append("This query has no configuration options.");
        }
        else
        {
            builder.append("Configuration options:")
                    .append(lineSeparator())
                    .append(lineSeparator());
            config.forEach((name, description) ->
                    builder.append("- **")
                            .append(name)
                            .append("**: ")
                            .append(description)
                            .append(lineSeparator()));
        }
        var errorMessage = builder.toString();
        return failure(errorMessage);
    }
}

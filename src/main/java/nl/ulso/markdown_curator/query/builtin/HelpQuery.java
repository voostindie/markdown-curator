package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.query.*;

import java.util.Map;

import static java.lang.System.lineSeparator;

public final class HelpQuery
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
        return "Shows detailed help information for a query.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
                "name", "Name of the query to get help for."
        );
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var queryName = definition.configuration().string("name", "help");
        var query = catalog.query(queryName);
        return () ->
        {
            var builder = new StringBuilder();
            builder.append("### ")
                    .append(queryName)
                    .append(lineSeparator())
                    .append(lineSeparator())
                    .append(query.description())
                    .append(lineSeparator())
                    .append(lineSeparator());
            var config = query.supportedConfiguration();
            if (config.isEmpty())
            {
                builder.append("This query has no configuration options.");
            }
            else
            {
                builder.append("Configuration options:")
                        .append(lineSeparator())
                        .append(lineSeparator());
                config.keySet().stream().sorted().forEach(name ->
                        builder.append("- **")
                                .append(name)
                                .append("**: ")
                                .append(config.get(name))
                                .append(lineSeparator()));
            }
            return builder.toString();
        };
    }
}

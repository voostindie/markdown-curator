package nl.ulso.markdown_curator.query;

import nl.ulso.markdown_curator.vault.QueryBlock;

import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyMap;
import static nl.ulso.markdown_curator.query.QueryResult.failure;

class UnknownQuery
        implements Query
{
    private final QueryCatalog catalog;
    private final String name;

    public UnknownQuery(QueryCatalog catalog, String name)
    {
        this.catalog = catalog;
        this.name = name;
    }

    @Override
    public String name()
    {
        return name;
    }

    public String description()
    {
        return "does nothing; only outputs instructions";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public QueryResult run(QueryBlock queryBlock)
    {
        var builder = new StringBuilder();
        builder.append("This vault has no query defined called '")
                .append(name)
                .append("'.")
                .append(lineSeparator());
        if (catalog.queries().size() == 1)
        {
            builder.append("Actually this vault has no queries defined at all.")
                    .append(lineSeparator())
                    .append("A developer has to build some first!")
                    .append(lineSeparator());
        }
        else
        {
            builder.append("Queries available in this vault are:")
                    .append(lineSeparator())
                    .append(lineSeparator());
            catalog.queries().forEach(specification ->
                    builder.append("- **")
                            .append(specification.name())
                            .append("**: ")
                            .append(specification.description())
                            .append(lineSeparator()));
        }
        builder.append(lineSeparator())
                .append("Use the 'help' query to get more information on a query.")
                .append(lineSeparator());
        var errorMessage = builder.toString();
        return failure(errorMessage);
    }
}

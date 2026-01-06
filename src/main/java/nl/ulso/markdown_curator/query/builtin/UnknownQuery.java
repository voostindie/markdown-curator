package nl.ulso.markdown_curator.query.builtin;

import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.query.*;

import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;

public final class UnknownQuery
        implements Query
{
    private final QueryCatalog catalog;
    private final String name;
    private final QueryResultFactory resultFactory;

    public UnknownQuery(QueryCatalog catalog, String name, QueryResultFactory resultFactory)
    {
        this.catalog = catalog;
        this.name = name;
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return name;
    }

    public String description()
    {
        return "Does nothing; only outputs instructions.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return false;
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var builder = new StringBuilder();
        if (catalog.isEmpty())
        {
            builder.append("This vault has no queries defined.")
                    .append(lineSeparator())
                    .append("A developer has to build and register some first...")
                    .append(lineSeparator());
        }
        else
        {
            builder.append("Queries available in this vault are:")
                    .append(lineSeparator())
                    .append(lineSeparator());
            catalog.queries().stream().sorted(comparing(Query::name)).forEach(specification ->
                    builder.append("- **")
                            .append(specification.name())
                            .append("**: ")
                            .append(specification.description())
                            .append(lineSeparator()));
        }
        builder.append(lineSeparator())
                .append("Use the 'help' query to get more information on a specific query.")
                .append(lineSeparator());
        return resultFactory.error(builder.toString());
    }
}

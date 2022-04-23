package nl.ulso.macu.query;

import nl.ulso.macu.vault.Dictionary;
import nl.ulso.macu.vault.Vault;

import java.util.*;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;

public class InMemoryQueryCatalog
        implements QueryCatalog
{
    private final Vault vault;
    private final Map<String, Query> queries;

    public InMemoryQueryCatalog(Vault vault)
    {
        this.vault = vault;
        this.queries = new HashMap<>();
    }

    @Override
    public void register(Query query)
    {
        queries.put(query.name(), query);
    }

    @Override
    public Collection<Query> queries()
    {
        return Collections.unmodifiableCollection(queries.values());
    }

    @Override
    public Query query(String name)
    {
        return queries.getOrDefault(name, new UnknownQuery(name));
    }

    private class UnknownQuery
            implements Query
    {
        private final String name;

        public UnknownQuery(String name)
        {
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
        public PreparedQuery prepare(Dictionary configuration)
        {
            return () -> new QueryResult() {
                @Override
                public boolean isSuccess()
                {
                    return false;
                }

                @Override
                public List<String> columns()
                {
                    return emptyList();
                }

                @Override
                public List<Map<String, String>> rows()
                {
                    return emptyList();
                }

                @Override
                public String errorMessage()
                {
                    var builder = new StringBuilder();
                    builder.append("This vault has no query defined called '")
                            .append(name)
                            .append("'.")
                            .append(lineSeparator());
                    if (queries().isEmpty())
                    {
                        builder.append("Actually this vault has no queries defined at all.")
                                .append(lineSeparator())
                                .append("A developer has to build some first!")
                                .append(lineSeparator());
                    }
                    else
                    {
                        builder.append("Queries known for this vault are:")
                                .append(lineSeparator())
                                .append(lineSeparator());
                        queries().forEach(specification ->
                                builder.append("- **")
                                        .append(specification.name())
                                        .append("**: ")
                                        .append(specification.description())
                                        .append(lineSeparator()));
                    }
                    return builder.toString();
                }
            };
        }
    }
}

package nl.ulso.macu.query;

import nl.ulso.macu.vault.Dictionary;

import java.util.*;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;

public class InMemoryQueryCatalog
        implements QueryCatalog
{
    private final Map<String, QuerySpecification> specifications;

    public InMemoryQueryCatalog()
    {
        this.specifications = new HashMap<>();
    }

    @Override
    public void register(QuerySpecification querySpecification)
    {
        specifications.put(querySpecification.type(), querySpecification);
    }

    @Override
    public Collection<QuerySpecification> specifications()
    {
        return Collections.unmodifiableCollection(specifications.values());
    }

    @Override
    public QuerySpecification specificationFor(String type)
    {
        return specifications.getOrDefault(type, new InvalidQuerySpecification(type));
    }

    private class InvalidQuerySpecification
            implements QuerySpecification
    {
        private final String unknownType;

        public InvalidQuerySpecification(String unknownType)
        {
            this.unknownType = unknownType;
        }

        @Override
        public String type()
        {
            return "none";
        }

        public String description()
        {
            return "does nothing; only outputs instructions";
        }

        @Override
        public QueryRunner configure(Dictionary configuration)
        {
            return r -> new QueryResult()
            {
                @Override
                public boolean isValid()
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
                            .append(unknownType)
                            .append("'.")
                            .append(lineSeparator());
                    if (specifications().isEmpty())
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
                        specifications().forEach(specification ->
                                builder.append("- **")
                                        .append(specification.type())
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

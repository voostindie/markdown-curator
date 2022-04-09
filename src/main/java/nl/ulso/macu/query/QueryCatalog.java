package nl.ulso.macu.query;

import java.util.Collection;

public interface QueryCatalog
{
    void register(QuerySpecification querySpecification);

    Collection<QuerySpecification> specifications();

    QuerySpecification specificationFor(String type);
}

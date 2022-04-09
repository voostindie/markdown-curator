package nl.ulso.obsidian.watcher.query;

import java.util.Collection;

public interface QueryCatalog
{
    void register(QuerySpecification querySpecification);

    Collection<QuerySpecification> specifications();

    QuerySpecification specificationFor(String type);
}

package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.query.*;

import javax.inject.Inject;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class NoOpQuery
        implements Query
{
    private final QueryResultFactory resultFactory;

    @Inject
    public NoOpQuery(QueryResultFactory resultFactory)
    {
        this.resultFactory = resultFactory;
    }

    @Override
    public String name()
    {
        return "noop";
    }

    @Override
    public String description()
    {
        return "does nothing and leaves content intact";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return emptyMap();
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        return resultFactory.noOp();
    }
}

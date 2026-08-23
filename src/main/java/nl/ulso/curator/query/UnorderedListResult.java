package nl.ulso.curator.query;

import java.util.List;

record UnorderedListResult(List<String> rows)
    implements QueryResult
{
}

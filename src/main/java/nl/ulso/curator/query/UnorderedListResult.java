package nl.ulso.curator.query;

import java.util.List;

public record UnorderedListResult(List<String> rows)
    implements QueryResult
{
}

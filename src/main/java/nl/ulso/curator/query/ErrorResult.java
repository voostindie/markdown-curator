package nl.ulso.curator.query;

public record ErrorResult(String errorMessage)
    implements QueryResult
{
}

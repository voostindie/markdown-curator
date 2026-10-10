package nl.ulso.curator.query;

record ErrorResult(String errorMessage)
    implements QueryResult
{
}

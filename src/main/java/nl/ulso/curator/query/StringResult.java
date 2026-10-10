package nl.ulso.curator.query;

class StringResult
    implements QueryResult
{
    private final String outputMessage;

    StringResult(String outputMessage)
    {
        this.outputMessage = outputMessage;
    }

    String outputMessage()
    {
        return outputMessage;
    }
}

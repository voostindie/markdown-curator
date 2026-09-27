package nl.ulso.curator.query;

public class StringResult
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

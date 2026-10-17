package nl.ulso.curator.query;

import jakarta.inject.Inject;
import jakarta.json.Json;
import nl.ulso.json.JsonToString;

final class ErrorJsonFormatter
    extends JsonFormatter
    implements QueryResultFormatter<ErrorResult>
{
    @Inject
    ErrorJsonFormatter()
    {
        // Default constructor for Dagger injection
    }

    @Override
    public Class<ErrorResult> queryResultType()
    {
        return ErrorResult.class;
    }

    @Override
    public String format(ErrorResult result)
    {
        var builder = Json.createObjectBuilder();
        builder.add("error", result.errorMessage());
        return JsonToString.jsonToString(builder.build());
    }
}

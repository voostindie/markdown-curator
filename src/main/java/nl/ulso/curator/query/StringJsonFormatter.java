package nl.ulso.curator.query;

import jakarta.inject.Inject;

import static jakarta.json.Json.createObjectBuilder;
import static nl.ulso.json.JsonToString.jsonToString;

final class StringJsonFormatter
    extends JsonFormatter
    implements QueryResultFormatter<StringResult>
{
    @Inject
    StringJsonFormatter()
    {
    }

    @Override
    public Class<StringResult> queryResultType()
    {
        return StringResult.class;
    }

    @Override
    public String format(StringResult result)
    {
        var json = createObjectBuilder()
            .add("value", result.outputMessage())
            .build();
        return jsonToString(json);
    }
}

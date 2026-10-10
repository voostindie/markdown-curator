package nl.ulso.curator.query;

import jakarta.inject.Inject;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static nl.ulso.json.JsonToString.jsonToString;

final class UnorderedListJsonFormatter
    extends JsonFormatter
    implements QueryResultFormatter<UnorderedListResult>
{
    @Inject
    UnorderedListJsonFormatter()
    {
    }

    @Override
    public Class<UnorderedListResult> queryResultType()
    {
        return UnorderedListResult.class;
    }

    @Override
    public String format(UnorderedListResult result)
    {
        var arrayBuilder = createArrayBuilder();
        for (String row : result.rows())
        {
            arrayBuilder.add(row);
        }
        var json = createObjectBuilder()
            .add("items", arrayBuilder)
            .build();
        return jsonToString(json);
    }
}

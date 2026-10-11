package nl.ulso.curator.query;

import jakarta.inject.Inject;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static nl.ulso.json.JsonToString.jsonToString;

/// An empty JSON result has the same structure as the [UnorderedListJsonFormatter] and
/// [TableJsonFormatter] for empty results sets: an object with an `items` array with zero items.`
final class EmptyJsonFormatter
    extends JsonFormatter
    implements QueryResultFormatter<EmptyResult>
{
    @Inject
    public EmptyJsonFormatter()
    {
    }

    @Override
    public Class<EmptyResult> queryResultType()
    {
        return EmptyResult.class;
    }

    @Override
    public String format(EmptyResult result)
    {
        var json = createObjectBuilder()
            .add("items", createArrayBuilder())
            .build();
        return jsonToString(json);
    }
}

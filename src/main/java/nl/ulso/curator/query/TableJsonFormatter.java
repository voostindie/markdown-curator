package nl.ulso.curator.query;

import jakarta.inject.Inject;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static nl.ulso.json.JsonToString.jsonToString;

final class TableJsonFormatter
    extends JsonFormatter
    implements QueryResultFormatter<TableResult>
{
    @Inject
    TableJsonFormatter()
    {
    }

    @Override
    public Class<TableResult> queryResultType()
    {
        return TableResult.class;
    }

    @Override
    public String format(TableResult result)
    {
        var columns = result.columns();
        var arrayBuilder = createArrayBuilder();
        for (var row : result.rows())
        {
            var objectBuilder = createObjectBuilder();
            for (String column : columns)
            {
                objectBuilder.add(column, row.get(column));
            }
            arrayBuilder.add(objectBuilder);
        }
        var json = createObjectBuilder().add("items", arrayBuilder).build();
        return jsonToString(json);
    }
}

package nl.ulso.curator.query;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import nl.ulso.dictionary.Dictionary;

import java.util.List;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.json.Json.createValue;
import static nl.ulso.curator.query.OutputFormat.JSON;

/// Base class for JSON formatters, with convenience methods.
public abstract class JsonFormatter
{
    public final OutputFormat outputFormat()
    {
        return JSON;
    }

    protected JsonValue jsonValueFor(Object object)
    {
        switch (object)
        {
            case null ->
            {
                return JsonValue.NULL;
            }
            case Double doubleValue ->
            {
                return createValue(doubleValue);
            }
            case Long longValue ->
            {
                return createValue(longValue);
            }
            case Integer intValue ->
            {
                return createValue(intValue);
            }
            case Boolean boolValue ->
            {
                return boolValue ? JsonValue.TRUE : JsonValue.FALSE;
            }
            case List<?> list ->
            {
                var listBuilder = createArrayBuilder();
                list.forEach(item -> listBuilder.add(jsonValueFor(item)));
                return listBuilder.build();
            }
            default ->
            {
                var stringValue = object.toString();
                if (stringValue.startsWith("[[") && stringValue.endsWith("]]"))
                {
                    return createValue(stringValue.substring(2, stringValue.length() - 2));
                }
                return createValue(stringValue);
            }
        }
    }

    protected String jsonPropertyName(String dictionaryPropertyName)
    {
        return dictionaryPropertyName.toLowerCase().replace('-', '_');
    }

    protected JsonObject jsonObjectFor(Dictionary dictionary)
    {
        var builder = createObjectBuilder();
        dictionary.propertyNames().forEach(property ->
            {
                var propertyName = jsonPropertyName(property);
                var value = dictionary.getProperty(property).orElseThrow(); // Can't throw
                builder.add(propertyName, jsonValueFor(value));
            }
        );
        return builder.build();
    }
}

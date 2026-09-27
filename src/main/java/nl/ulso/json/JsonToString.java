package nl.ulso.json;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.io.StringWriter;

public final class JsonToString
{
    private JsonToString()
    {
    }

    public static String jsonToString(JsonObject object)
    {
        try (
            var stringWriter = new StringWriter();
            var jsonWriter = Json.createWriter(stringWriter)
        )
        {
            jsonWriter.writeObject(object);
            return stringWriter.toString();
        }
        catch (IOException _)
        {
            return "";
        }
    }
}

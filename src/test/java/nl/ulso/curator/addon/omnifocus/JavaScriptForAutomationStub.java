package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.*;
import nl.ulso.jxa.JavaScriptForAutomation;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

@Singleton
final class JavaScriptForAutomationStub
    implements JavaScriptForAutomation
{
    private static JsonArray EXPECTED_RESULT;
    static {
        setExpectedResult(emptyList());
    }

    @Inject
    JavaScriptForAutomationStub()
    {
    }

    static void setExpectedResult(List<Map<String, Object>> list)
    {
        if (list == null)
        {
            EXPECTED_RESULT = null;
            return;
        }
        var arrayBuilder = Json.createArrayBuilder();
        for (var item : list)
        {
            arrayBuilder.add(Json.createObjectBuilder(item).build());
        }
        EXPECTED_RESULT = arrayBuilder.build();
    }

    @Override
    public JsonObject runScriptForObject(String name, String... arguments)
    {
        throw new UnsupportedOperationException("Not supported in this stub!");
    }

    @Override
    public JsonArray runScriptForArray(String name, String... arguments)
    {
        if (EXPECTED_RESULT == null)
        {
            throw new IllegalStateException("No JSON Array available.");
        }
        return EXPECTED_RESULT;
    }
}

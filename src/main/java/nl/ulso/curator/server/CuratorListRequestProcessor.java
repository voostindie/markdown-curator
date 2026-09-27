package nl.ulso.curator.server;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.Curator;
import nl.ulso.curator.query.OutputFormat;
import nl.ulso.dictionary.Dictionary;

import java.util.List;
import java.util.Map;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static nl.ulso.curator.query.OutputFormat.JSON;
import static nl.ulso.curator.server.Result.ok;
import static nl.ulso.json.JsonToString.jsonToString;

/// Lists all curators available in the system in JSON format.
@Singleton
final class CuratorListRequestProcessor
    implements RequestProcessor
{
    private final List<String> curatorNames;

    @Inject
    CuratorListRequestProcessor(Map<String, Curator> curators)
    {
        this.curatorNames = curators.keySet().stream()
            .map(String::toLowerCase)
            .sorted()
            .toList();
    }

    @Override
    public String pathTemplate()
    {
        return "/curators";
    }

    @Override
    public Result process(
        Dictionary pathParameters,
        Dictionary queryParameters,
        OutputFormat requestedOutputFormat)
    {
        var json = createObjectBuilder()
            .add("curators", createArrayBuilder(curatorNames))
            .build();
        return ok(JSON, jsonToString(json));
    }
}

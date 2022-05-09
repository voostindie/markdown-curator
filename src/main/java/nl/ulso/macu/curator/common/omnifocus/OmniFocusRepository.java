package nl.ulso.macu.curator.common.omnifocus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.util.*;

/**
 * WARNING: this repository works ONLY on my machine, which is fine for now.
 * <p/>
 * This repository using macOS scripting to fetch a list of projects from OmniFocus. Because
 * Obsidian saves files often, queries are executed one every change and fetching projects is
 * blocking I/O, projects are refreshed with a minimum interval.
 * <p/>
 * Even though this class is reusable across curators, it is not thread-safe! Each curator requires
 * its own instance
 * <p/>
 * Much needed improvements are factoring out the osascript code, make it run a script available
 * on the classpath, and improving resilience.
 */
public class OmniFocusRepository
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OmniFocusRepository.class);
    private static final String COMMAND = "/usr/bin/osascript";
    private static final String SCRIPT =
            "/Users/vincent/Code/markdown-curator/src/main/jxa/omnifocus-projects.js";

    private final List<OmniFocusProject> projects;
    private final Map<String, Long> lastUpdated;

    public OmniFocusRepository()
    {
        projects = new ArrayList<>();
        lastUpdated = new HashMap<>();
    }

    public List<OmniFocusProject> projects(String folder, int refreshInterval)
    {
        refresh(folder, refreshInterval);
        return projects;
    }

    private void refresh(String folder, long interval)
    {
        long lastUpdate = lastUpdated.getOrDefault(folder, 0L);
        if (System.currentTimeMillis() - interval < lastUpdate)
        {
            return;
        }
        LOGGER.debug("Refreshing OmniFocus projects in folder '{}'", folder);
        projects.clear();
        try
        {
            var process = new ProcessBuilder(COMMAND, SCRIPT, folder).start();
            JsonArray array;
            try (
                    var parser = Json.createReader(new BufferedReader(
                            new InputStreamReader(process.getInputStream()))))
            {
                array = parser.readArray();
            }
            for (JsonValue value : array)
            {
                var object = value.asJsonObject();
                var id = object.getString("id");
                var name = object.getString("name");
                projects.add(new OmniFocusProject(id, name));
            }
        }
        catch (IOException | JsonParsingException e)
        {
            LOGGER.info("OmniFocus projects couldn't be fetched. List will be empty.");
        }
        lastUpdated.put(folder, System.currentTimeMillis());
    }
}
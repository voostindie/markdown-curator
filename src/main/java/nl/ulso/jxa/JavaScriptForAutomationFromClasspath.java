package nl.ulso.jxa;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;

/// Runs JavaScript for Automation scripts located on the classpath.
///
/// Source scripts are resolved from the classpath in the `/jxa` folder. To load and execute scripts
/// efficiently, the sources are first compiled. Compilation only happens when the compiled script
/// is somehow no longer available in the temporary directory where compiled scripts are stored.
///
/// This code can safely run in a multithreaded environment, *but* under high load the same script
/// may be compiled more than once; the code in this class doesn't protect against that. It's a bit
/// of a waste, bot not harmful otherwise, and not worth the extra complexity in code IMHO. In the
/// end exactly one compiled script will "win" and be reused for all executions of the same script.
///
/// Script compilation may take some time, and I've experienced cases where it just never finished
/// (I think...). To protect against that the compiler is not allowed to run for longer than
/// `MAX_COMPILATION_TIME_SECONDS`.
///
/// Running external scripts is inherently insecure. The code in this class aims to protect against
/// abuse by only running scripts from a save location: the application bundle. However, compiled
/// scripts are stored in the user's temporary directory. These scripts could be replaced. The code
/// does not protect against that.
@Singleton
final class JavaScriptForAutomationFromClasspath
    implements JavaScriptForAutomation
{
    private static final Logger LOGGER =
        LoggerFactory.getLogger(JavaScriptForAutomationFromClasspath.class);
    private static final String SOURCE_PACKAGE = "/jxa/";
    private static final String SOURCE_EXTENSION = ".js";
    private static final String COMPILED_SCRIPT_EXTENSION = ".scpt";
    private static final String COMPILE = "/usr/bin/osacompile";
    private static final String EXECUTE = "/usr/bin/osascript";
    private static final int MAX_COMPILATION_TIME_SECONDS = 5;

    private final ConcurrentMap<String, Path> scriptCache = new ConcurrentHashMap<>();

    @Inject
    JavaScriptForAutomationFromClasspath()
    {
    }

    @Override
    public JsonObject runScriptForObject(String name, String... arguments)
    {
        return runScript(name, arguments, JsonReader::readObject);
    }

    @Override
    public JsonArray runScriptForArray(String name, String... arguments)
    {
        return runScript(name, arguments, JsonReader::readArray);
    }

    private <J extends JsonStructure> J runScript(
        String name,
        String[] arguments,
        Function<JsonReader, J> jsonProcessor)
    {
        var path = resolveCompiledScript(name);
        var command = new ArrayList<>(List.of(EXECUTE, path.toString()));
        if (arguments != null)
        {
            command.addAll(asList(arguments));
        }
        try
        {
            LOGGER.debug("Running compiled script '{}' from path '{}'.", name, path);
            var process = new ProcessBuilder(command).start();
            try (var reader = process.inputReader())
            {
                var output = reader.lines().collect(joining(lineSeparator()));
                var jsonReader = Json.createReader(new StringReader(output));
                return jsonProcessor.apply(jsonReader);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Couldn't run compiled script: " + name, e);
        }
        catch (JsonParsingException e)
        {
            throw new IllegalStateException("Couldn't parse script output: " + name, e);
        }
    }

    private Path resolveCompiledScript(String scriptName)
    {
        var scriptPath = scriptCache.get(scriptName);
        if (scriptPath != null && Files.exists(scriptPath))
        {
            LOGGER.trace("Reusing compiled script '{}' from path '{}'.", scriptName, scriptPath);
            return scriptPath;
        }
        scriptCache.remove(scriptName);
        return scriptCache.computeIfAbsent(scriptName,
            name -> compileScript(name, loadScriptSource(scriptName))
        );
    }

    private List<String> loadScriptSource(String scriptName)
    {
        String sourcePath = SOURCE_PACKAGE + scriptName + SOURCE_EXTENSION;
        LOGGER.debug("Loading script '{}' from classpath '{}'.", scriptName, sourcePath);
        try (var inputStream = this.getClass().getResourceAsStream(sourcePath))
        {
            if (inputStream == null)
            {
                throw new IllegalStateException("Couldn't locate script: " + scriptName);
            }
            try (var reader = new BufferedReader(new InputStreamReader(inputStream)))
            {
                return reader.lines().toList();
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Couldn't load script: " + scriptName, e);
        }
    }

    private Path compileScript(String scriptName, List<String> source)
    {
        try
        {
            var outputFile = File.createTempFile(scriptName + "-", COMPILED_SCRIPT_EXTENSION);
            var outputPath = outputFile.toPath();
            LOGGER.debug("Compiling script '{}' to path '{}'.", scriptName, outputPath);
            var process = new ProcessBuilder(
                COMPILE,
                "-l", "JavaScript",
                "-o", outputPath.toString(),
                "-"
            ).start();
            try (var writer = process.outputWriter())
            {
                for (String line : source)
                {
                    writer.write(line);
                    writer.newLine();
                }
                writer.flush();
            }
            process.waitFor(MAX_COMPILATION_TIME_SECONDS, SECONDS);
            return outputPath;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Couldn't compile script: " + scriptName, e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Couldn't compile script: " + scriptName, e);
        }
    }
}

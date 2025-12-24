package nl.ulso.jxa;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/// Runs JavaScript for Automation scripts, returning the output as JSON objects.
///
/// This interface is not meant to be able to run scripts from just anywhere. All the interface
/// supports is a script "name". Implementations must resolve this name to an actual script.
public interface JavaScriptForAutomation
{
    /// Runs a script and interprets its output as a JSON object.
    ///
    /// @param name      Name of the script to run without path and extension, e.g. `hello`.
    /// @param arguments Arguments to pass to the script.
    /// @return The output of the script, parsed into a JSON object.
    JsonObject runScriptForObject(String name, String... arguments);

    /// Runs a script and interprets its output as a JSON array.
    ///
    /// @param name      Name of the script to run without path and extension, e.g. `hello`.
    /// @param arguments Arguments to pass to the script.
    /// @return The output of the script, parsed into a JSON array.
    JsonArray runScriptForArray(String name, String... arguments);
}

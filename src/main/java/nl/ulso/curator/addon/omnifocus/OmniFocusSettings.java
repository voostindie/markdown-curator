package nl.ulso.curator.addon.omnifocus;

import nl.ulso.dictionary.Dictionary;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static nl.ulso.curator.addon.omnifocus.OmniFocusExtensionFactory.*;

/// Settings for the OmniFocus extension; see [OmniFocusExtensionFactory].
///
/// At the least you must specify the folder in OmniFocus that holds the projects to fetch and
/// compare. Additionally, you can specify a predicate that each project name from OmniFocus is
/// tested against for inclusion, and the location of the OmniFocus database, which defaults to
/// [#DEFAULT_DATABASE].
///
/// Package-private: the extension receives its raw configuration, and [OmniFocusModule] turns it
/// into settings.
record OmniFocusSettings(
    String omniFocusFolder,
    Predicate<String> includePredicate,
    Path database)
{
    /// Location of the OmniFocus 4 database on macOS.
    public static final Path DEFAULT_DATABASE = Path.of(
        System.getProperty("user.home"),
        "Library",
        "Containers",
        "com.omnigroup.OmniFocus4",
        "Data",
        "Library",
        "Application Support",
        "OmniFocus",
        "OmniFocus.ofocus"
    );

    public OmniFocusSettings(String omniFocusFolder, Predicate<String> includePredicate)
    {
        this(omniFocusFolder, includePredicate, DEFAULT_DATABASE);
    }

    public OmniFocusSettings(String omniFocusFolder)
    {
        this(omniFocusFolder, _ -> true);
    }

    /// Fails if the configuration is invalid. Dagger creates the settings lazily, so
    /// [OmniFocusExtensionFactory] calls this up front to fail while creating the extension.
    static void validate(Dictionary configuration)
    {
        var folder = configuration.string(FOLDER, null);
        if (folder == null || folder.isBlank())
        {
            throw new IllegalArgumentException("Setting '" + FOLDER + "' is required.");
        }
    }

    static OmniFocusSettings from(Dictionary configuration)
    {
        validate(configuration);
        var folder = configuration.string(FOLDER, null);
        var excludedPrefixes = List.copyOf(configuration.listOfStrings(EXCLUDE_PREFIXES));
        var excludedNames = Set.copyOf(configuration.listOfStrings(EXCLUDE));
        var database = configuration.string(DATABASE, null);
        return new OmniFocusSettings(folder, name ->
            excludedPrefixes.stream().noneMatch(name::startsWith) &&
            !excludedNames.contains(name),
            database == null || database.isBlank() ? DEFAULT_DATABASE : Path.of(database)
        );
    }
}

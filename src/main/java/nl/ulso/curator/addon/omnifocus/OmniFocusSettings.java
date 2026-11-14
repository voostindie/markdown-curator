package nl.ulso.curator.addon.omnifocus;

import java.nio.file.Path;
import java.util.function.Predicate;

/// Settings for the OmniFocus extension; see [OmniFocusExtensionFactory].
///
/// At the least you must specify the folder in OmniFocus that holds the projects to fetch and
/// compare. Additionally, you can specify a predicate that each project name from OmniFocus is
/// tested against for inclusion, and the location of the OmniFocus database, which defaults to
/// [#DEFAULT_DATABASE].
public record OmniFocusSettings(
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
}

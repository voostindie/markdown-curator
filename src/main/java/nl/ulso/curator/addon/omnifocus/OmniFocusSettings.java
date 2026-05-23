package nl.ulso.curator.addon.omnifocus;

import java.util.function.Predicate;

/// Settings for the [OmniFocusModule].
///
/// At the least you must specify the folder in OmniFocus that holds the projects to fetch and
/// compare. Additionally, you can specify a predicate that each project name from OmniFocus is
/// tested against for inclusion.
public record OmniFocusSettings(
    String omniFocusFolder,
    Predicate<String> includePredicate)
{
    public OmniFocusSettings(String omniFocusFolder)
    {
        this(omniFocusFolder, projectName -> true);
    }
}

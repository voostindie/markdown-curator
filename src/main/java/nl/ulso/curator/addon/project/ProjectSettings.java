package nl.ulso.curator.addon.project;

import nl.ulso.dictionary.Dictionary;

import static nl.ulso.curator.addon.project.ProjectExtensionFactory.FOLDER;

/// Settings of the project extension; see [ProjectExtensionFactory]. Package-private: the
/// extension receives its raw configuration, and [ProjectModule] turns it into settings.
record ProjectSettings(String projectFolderName)
{
    static ProjectSettings from(Dictionary configuration)
    {
        return new ProjectSettings(configuration.string(FOLDER, "Projects"));
    }
}

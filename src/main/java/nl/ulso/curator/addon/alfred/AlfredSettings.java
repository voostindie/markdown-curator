package nl.ulso.curator.addon.alfred;

import nl.ulso.dictionary.Dictionary;

import static nl.ulso.curator.addon.alfred.AlfredExtensionFactory.*;

/// Settings of the Alfred extension; see [AlfredExtensionFactory]. All paths are optional and may
/// be `null`. The links section defaults to "Links" if `null`. Package-private: the extension
/// receives its raw configuration, and [AlfredModule] turns it into settings.
record AlfredSettings(
    String contactsPath,
    String projectsPath,
    String remindersPath,
    String systemsPath,
    String teamsPath,
    String thirdPartiesPath,
    String linksSection)
{
    static final String DEFAULT_LINKS_SECTION = "Links";

    AlfredSettings
    {
        if (linksSection == null)
        {
            linksSection = DEFAULT_LINKS_SECTION;
        }
    }

    static AlfredSettings from(Dictionary configuration)
    {
        return new AlfredSettings(
            configuration.string(CONTACTS_PATH, null),
            configuration.string(PROJECTS_PATH, null),
            configuration.string(REMINDERS_PATH, null),
            configuration.string(SYSTEMS_PATH, null),
            configuration.string(TEAMS_PATH, null),
            configuration.string(THIRD_PARTIES_PATH, null),
            configuration.string(LINKS_SECTION, null)
        );
    }
}

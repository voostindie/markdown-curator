package nl.ulso.curator.addon.journal;

import nl.ulso.dictionary.Dictionary;

import java.time.temporal.WeekFields;

import static nl.ulso.curator.addon.journal.JournalExtensionFactory.*;

/// Settings of the journal extension; see [JournalExtensionFactory]. Package-private: the
/// extension receives its raw configuration, and [JournalModule] turns it into settings.
record JournalSettings(String journalFolderName, String markerSubFolderName,
                       String activitiesSectionName, String projectFolderName,
                       WeekFields weekFields)
{
    JournalSettings(
            String journalFolderName, String markerSubFolderName, String activitiesSectionName,
            String projectFolderName)
    {
        this(journalFolderName, markerSubFolderName, activitiesSectionName, projectFolderName,
                WeekFields.ISO);
    }

    static JournalSettings from(Dictionary configuration)
    {
        return new JournalSettings(
            configuration.string(FOLDER, "Journal"),
            configuration.string(MARKERS_FOLDER, "Markers"),
            configuration.string(ACTIVITIES_SECTION, "Activities"),
            configuration.string(PROJECTS_FOLDER, "Projects")
        );
    }
}

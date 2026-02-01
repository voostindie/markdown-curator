package nl.ulso.curator.addon.journal;

import java.time.temporal.WeekFields;

/// Defines settings for the Journal feature.
///
/// Make sure to include an instance of this class in your Guice context!
public record JournalSettings(String journalFolderName, String markerSubFolderName,
                       String activitiesSectionName, String projectFolderName,
                       WeekFields weekFields)
{
    public JournalSettings(
            String journalFolderName, String markerSubFolderName, String activitiesSectionName,
            String projectFolderName)
    {
        this(journalFolderName, markerSubFolderName, activitiesSectionName, projectFolderName,
                WeekFields.ISO);
    }
}

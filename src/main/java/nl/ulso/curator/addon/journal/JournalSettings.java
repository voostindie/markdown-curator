package nl.ulso.curator.addon.journal;

import java.time.temporal.WeekFields;

/// Defines settings for the Journal feature.
///
/// Settings of the journal extension; see [JournalExtensionFactory].
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

package nl.ulso.markdown_curator.journal;

/**
 * Defines settings for the Journal feature.
 * <p/>
 * Make sure to include an instance of this class in your Guice context!
 */
public class JournalSettings
{
    private final String journalFolderName;
    private final String activitiesSectionName;

    public JournalSettings(String journalFolderName, String activitiesSectionName)
    {
        this.journalFolderName = journalFolderName;
        this.activitiesSectionName = activitiesSectionName;
    }

    /**
     * @return The name of the folder - directly under the root - that contains all journal
     * entries, e.g. "Journal".
     */
    public String journalFolderName()
    {
        return journalFolderName;
    }

    /**
     * @return The name of the level-2 section in each daily note that contains the activity
     * outline, e.g. "Activities".
     */
    public String activitiesSectionName()
    {
        return activitiesSectionName;
    }
}

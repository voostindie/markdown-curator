package nl.ulso.curator.addon.projectjournal;

import dagger.Module;
import dagger.Provides;
import nl.ulso.curator.addon.journal.JournalSettings;
import nl.ulso.curator.addon.project.ProjectSettings;

@Module
abstract class ProjectJournalTestModule
{
    @Provides
    static ProjectSettings provideProjectSettings()
    {
        return new ProjectSettings("Projects");
    }

    @Provides
    static JournalSettings provideJournalSettings()
    {
        return new JournalSettings(
            "Journal",
            "Markers",
            "Activities",
            "Projects"
        );
    }
}

package nl.ulso.curator.addon.journal;

import jakarta.inject.Inject;
import nl.ulso.curator.extension.*;
import nl.ulso.dictionary.Dictionary;

import java.util.Map;
import java.util.Set;

/// Creates the journal extension. All settings are optional:
///
/// ```yaml
/// extensions:
///   journal:
///     folder: Journal
///     markers-folder: Markers
///     activities-section: Activities
///     projects-folder: Projects
/// ```
final class JournalExtensionFactory
    implements ExtensionFactory
{
    static final String FOLDER = "folder";
    static final String MARKERS_FOLDER = "markers-folder";
    static final String ACTIVITIES_SECTION = "activities-section";
    static final String PROJECTS_FOLDER = "projects-folder";

    private final JournalExtension.Factory factory;

    @Inject
    JournalExtensionFactory(JournalExtension.Factory factory)
    {
        this.factory = factory;
    }

    @Override
    public String description()
    {
        return "Journal extracted from daily notes, with markers, timelines and navigation.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
            FOLDER, "Folder with daily and weekly notes; defaults to `Journal`.",
            MARKERS_FOLDER, "Subfolder of the journal folder with markers; defaults to `Markers`.",
            ACTIVITIES_SECTION, "Section in daily notes with activities; defaults to `Activities`.",
            PROJECTS_FOLDER, "Folder with project documents; defaults to `Projects`."
        );
    }

    @Override
    public Set<Class<?>> provides()
    {
        return Set.of(Journal.class);
    }

    @Override
    public Extension create(Dictionary configuration, ExtensionContext context)
    {
        return factory.create(configuration);
    }
}

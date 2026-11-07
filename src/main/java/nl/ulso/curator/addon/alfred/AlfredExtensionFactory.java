package nl.ulso.curator.addon.alfred;

import jakarta.inject.Inject;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.extension.*;
import nl.ulso.dictionary.Dictionary;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/// Creates the Alfred extension. It requires the journal extension. All settings are optional:
///
/// ```yaml
/// extensions:
///   alfred:
///     contacts-path: Contacts
///     projects-path: Projects
///     reminders-path: Reminders
///     systems-path: Systems
///     teams-path: Teams
///     third-parties-path: 3rd Parties
///     links-section: Links
/// ```
final class AlfredExtensionFactory
    implements ExtensionFactory
{
    static final String CONTACTS_PATH = "contacts-path";
    static final String PROJECTS_PATH = "projects-path";
    static final String REMINDERS_PATH = "reminders-path";
    static final String SYSTEMS_PATH = "systems-path";
    static final String TEAMS_PATH = "teams-path";
    static final String THIRD_PARTIES_PATH = "third-parties-path";
    static final String LINKS_SECTION = "links-section";

    private final AlfredExtension.Factory factory;

    @Inject
    AlfredExtensionFactory(AlfredExtension.Factory factory)
    {
        this.factory = factory;
    }

    @Override
    public String description()
    {
        return "Exposes curator settings and external links to the Alfred workflow.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
            CONTACTS_PATH, "Folder with contacts.",
            PROJECTS_PATH, "Folder with projects.",
            REMINDERS_PATH, "Folder with reminders.",
            SYSTEMS_PATH, "Folder with systems.",
            TEAMS_PATH, "Folder with teams.",
            THIRD_PARTIES_PATH, "Folder with third parties.",
            LINKS_SECTION, "Section in documents with external links; defaults to `Links`."
        );
    }

    @Override
    public Set<Class<?>> requires()
    {
        return Set.of(Journal.class);
    }

    @Override
    public Extension create(Dictionary configuration, Services services)
    {
        var settings = AlfredSettings.builder()
            .contactsPath(configuration.string(CONTACTS_PATH, null))
            .projectsPath(configuration.string(PROJECTS_PATH, null))
            .remindersPath(configuration.string(REMINDERS_PATH, null))
            .systemsPath(configuration.string(SYSTEMS_PATH, null))
            .teamsPath(configuration.string(TEAMS_PATH, null))
            .thirdPartiesPath(configuration.string(THIRD_PARTIES_PATH, null))
            .build();
        return factory.create(
            settings,
            services.get(Journal.class),
            Optional.ofNullable(configuration.string(LINKS_SECTION, null))
        );
    }
}

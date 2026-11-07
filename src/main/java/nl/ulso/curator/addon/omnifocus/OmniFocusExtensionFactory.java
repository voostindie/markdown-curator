package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import nl.ulso.curator.addon.project.ProjectAttributeDefinition;
import nl.ulso.curator.addon.project.ProjectRepository;
import nl.ulso.curator.extension.*;
import nl.ulso.dictionary.Dictionary;

import java.util.*;

import static nl.ulso.curator.addon.omnifocus.OmniFocusProjectAttributeValueProducer.OMNIFOCUS_URL_ATTRIBUTE;
import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.byFrontMatterProperty;
import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.newAttributeDefinition;
import static nl.ulso.curator.extension.Contribution.contribution;

/// Creates the OmniFocus extension. It requires the project extension, and contributes the
/// `omnifocus` project attribute: the URL of the project in OmniFocus.
///
/// ```yaml
/// extensions:
///   omnifocus:
///     folder: My folder
///     exclude-prefixes:
///       - ⚡️
///     exclude:
///       - 🤖 Routine
/// ```
final class OmniFocusExtensionFactory
    implements ExtensionFactory
{
    static final String FOLDER = "folder";
    static final String EXCLUDE_PREFIXES = "exclude-prefixes";
    static final String EXCLUDE = "exclude";

    private final OmniFocusExtension.Factory factory;

    @Inject
    OmniFocusExtensionFactory(OmniFocusExtension.Factory factory)
    {
        this.factory = factory;
    }

    @Override
    public String description()
    {
        return "Compares projects in the vault with projects in OmniFocus.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of(
            FOLDER, "Folder in OmniFocus with the projects; required.",
            EXCLUDE_PREFIXES, "List of prefixes of OmniFocus projects to ignore.",
            EXCLUDE, "List of names of OmniFocus projects to ignore."
        );
    }

    @Override
    public Set<Class<?>> requires()
    {
        return Set.of(ProjectRepository.class);
    }

    @Override
    public List<Contribution> contributions(Dictionary configuration)
    {
        return List.of(contribution(ProjectAttributeDefinition.class,
            newAttributeDefinition(String.class, OMNIFOCUS_URL_ATTRIBUTE)));
    }

    @Override
    public Extension create(Dictionary configuration, Services services)
    {
        return factory.create(
            settingsFrom(configuration),
            services.get(ProjectRepository.class),
            byFrontMatterProperty(services.all(ProjectAttributeDefinition.class))
        );
    }

    static OmniFocusSettings settingsFrom(Dictionary configuration)
    {
        var folder = configuration.string(FOLDER, null);
        if (folder == null || folder.isBlank())
        {
            throw new IllegalArgumentException("Setting '" + FOLDER + "' is required.");
        }
        var excludedPrefixes = List.copyOf(configuration.listOfStrings(EXCLUDE_PREFIXES));
        var excludedNames = Set.copyOf(configuration.listOfStrings(EXCLUDE));
        return new OmniFocusSettings(folder, name ->
            excludedPrefixes.stream().noneMatch(name::startsWith) &&
            !excludedNames.contains(name));
    }
}

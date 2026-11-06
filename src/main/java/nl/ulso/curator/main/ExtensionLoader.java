package nl.ulso.curator.main;

import jakarta.inject.*;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.change.*;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.extension.ExtensionFactory;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.Vault;
import nl.ulso.dictionary.Dictionary;
import org.slf4j.Logger;

import java.util.*;

import static java.util.stream.Collectors.joining;
import static nl.ulso.curator.CuratorModule.CONFIGURATION_DOCUMENT_KEY;
import static nl.ulso.curator.change.Change.Kind.DELETE;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.change.Changelog.emptyChangelog;
import static nl.ulso.curator.change.Reset.RESET;
import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static nl.ulso.dictionary.Dictionary.mapDictionary;
import static org.slf4j.LoggerFactory.getLogger;

/// Special change processor that loads [Extension]s based on the configuration document, which
/// is only enabled if it is configured in the curator module, under the name
/// [CuratorModule#CONFIGURATION_DOCUMENT_KEY].
///
/// The configuration is read from the `extensions` property in the front matter of the
/// configuration document. It's either a list of extension names, or a map of extension names to
/// their configuration:
///
/// ```yaml
/// extensions:
///   directory:
///     teams-folder: Teams
///   another-extension:
/// ```
///
/// The configuration is (re)loaded:
///
/// - On every [Reset]; that is: at startup, and when the watch document is touched.
/// - Whenever the configuration document changes, but only if the `extensions` property changed.
/// In this case the loader publishes a [Reset], which triggers a complete rebuild of all state in
/// all change processors, including the new ones.
///
/// The loader doesn't activate the extensions itself. It stages them in the [ExtensionRegistry];
/// the [DefaultChangeProcessorOrchestrator] activates them.
@Singleton
final class ExtensionLoader
    implements ChangeProcessor
{
    static final String EXTENSIONS_PROPERTY = "extensions";

    private static final Logger LOGGER = getLogger(ExtensionLoader.class);

    private final String configurationDocumentName;
    private final Vault vault;
    private final Map<String, ExtensionFactory> extensionFactories;
    private final ExtensionRegistry registry;
    private Object lastConfiguration;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    ExtensionLoader(
        @Named(CONFIGURATION_DOCUMENT_KEY) Optional<String> configurationDocumentName,
        Vault vault,
        Map<String, ExtensionFactory> extensionFactories,
        ExtensionRegistry registry)
    {
        this.configurationDocumentName = configurationDocumentName.orElse(null);
        this.vault = vault;
        this.extensionFactories = Map.copyOf(extensionFactories);
        this.registry = registry;
        this.lastConfiguration = null;
        if (this.configurationDocumentName != null)
        {
            LOGGER.debug("Configuration document configured: '{}'.",
                this.configurationDocumentName
            );
        }
        else if (!extensionFactories.isEmpty())
        {
            LOGGER.warn("Extensions are available, but no configuration document is configured. " +
                        "No extensions will be loaded.");
        }
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Reset.class, Document.class);
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(Reset.class);
    }

    @Override
    public Changelog apply(Changelog changelog)
    {
        if (configurationDocumentName == null)
        {
            return emptyChangelog();
        }
        if (changelog.changes().anyMatch(isPayloadType(Reset.class)))
        {
            LOGGER.debug("Reset detected. (Re)loading extensions from the configuration document.");
            loadConfiguration(readConfiguration(vault.findDocument(configurationDocumentName)));
            return emptyChangelog();
        }
        var lastChange = changelog.changesFor(Document.class)
            .filter(change -> change.value().name().equals(configurationDocumentName))
            .reduce((_, second) -> second);
        if (lastChange.isEmpty())
        {
            return emptyChangelog();
        }
        var change = lastChange.get();
        var document = change.kind() == DELETE
                       ? Optional.<Document>empty()
                       : Optional.of(change.value());
        var configuration = readConfiguration(document);
        if (Objects.equals(configuration, lastConfiguration))
        {
            LOGGER.debug("Configuration document changed, but the extensions didn't.");
            return emptyChangelog();
        }
        LOGGER.info("Configuration of extensions has changed. Forcing a complete refresh.");
        if (loadConfiguration(configuration))
        {
            return changelogFor(RESET);
        }
        return emptyChangelog();
    }

    private Object readConfiguration(Optional<Document> document)
    {
        return document
            .flatMap(d -> d.frontMatter().getProperty(EXTENSIONS_PROPERTY))
            .orElse(null);
    }

    /// Creates all extensions from the configuration and stages them in the registry.
    ///
    /// @return `true` if the extensions were staged, `false` if the configuration was rejected.
    private boolean loadConfiguration(Object configuration)
    {
        lastConfiguration = configuration;
        try
        {
            var extensions = createExtensions(parseConfiguration(configuration));
            registry.stage(extensions);
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info("Staged {} extension(s): {}.", extensions.size(),
                    String.join(", ", new TreeSet<>(extensions.keySet())));
            }
            return true;
        }
        catch (RuntimeException e)
        {
            LOGGER.error("Rejecting the extension configuration. Keeping the current extensions.",
                e
            );
            registry.reject(e.getMessage());
            return false;
        }
    }

    /// Parses the raw configuration from the front matter into a map of extension names to their
    /// configuration; the map is ordered for predictability.
    private static SortedMap<String, Dictionary> parseConfiguration(Object configuration)
    {
        var result = new TreeMap<String, Dictionary>();
        switch (configuration)
        {
            case null ->
            {
                // No extensions configured.
            }
            case Collection<?> list -> list.forEach(name ->
                result.put(String.valueOf(name), emptyDictionary()));
            case Map<?, ?> map -> map.forEach((name, value) ->
                result.put(String.valueOf(name), toDictionary(String.valueOf(name), value)));
            default -> throw new IllegalArgumentException(
                "Property '" + EXTENSIONS_PROPERTY + "' must be a list or a map.");
        }
        return result;
    }

    private static Dictionary toDictionary(String extensionName, Object value)
    {
        return switch (value)
        {
            case null -> emptyDictionary();
            case Map<?, ?> map ->
            {
                var copy = new HashMap<String, Object>();
                map.forEach((key, v) -> copy.put(String.valueOf(key), v));
                yield mapDictionary(copy);
            }
            default -> throw new IllegalArgumentException(
                "Configuration of extension '" + extensionName + "' must be a map.");
        };
    }

    private Map<String, Extension> createExtensions(SortedMap<String, Dictionary> configuration)
    {
        var unknown = configuration.keySet().stream()
            .filter(name -> !extensionFactories.containsKey(name))
            .toList();
        if (!unknown.isEmpty())
        {
            throw new IllegalArgumentException(
                "Unknown extension(s): " + String.join(", ", unknown) + ". Available: " +
                extensionFactories.keySet().stream().sorted().collect(joining(", ")) + ".");
        }
        var extensions = new HashMap<String, Extension>(configuration.size());
        configuration.forEach((name, settings) -> {
            try
            {
                extensions.put(name, extensionFactories.get(name).create(settings));
            }
            catch (RuntimeException e)
            {
                throw new IllegalArgumentException(
                    "Could not create extension '" + name + "': " + e.getMessage(), e);
            }
        });
        return extensions;
    }
}

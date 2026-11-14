package nl.ulso.curator.main;

import jakarta.inject.*;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.change.*;
import nl.ulso.curator.extension.Extension;
import nl.ulso.curator.vault.*;
import nl.ulso.dictionary.Dictionary;
import org.slf4j.Logger;

import java.util.*;

import static nl.ulso.curator.CuratorModule.CONFIGURATION_DOCUMENT_KEY;
import static nl.ulso.curator.change.Change.Kind.DELETE;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.change.Changelog.emptyChangelog;
import static nl.ulso.curator.change.Reset.RESET;
import static nl.ulso.curator.main.CuratorSettings.DEFAULT_DELAY_IN_SECONDS;
import static nl.ulso.curator.main.CuratorSettings.DEFAULT_LOCALE;
import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static nl.ulso.dictionary.Dictionary.yamlDictionary;
import static org.slf4j.LoggerFactory.getLogger;

/// Special change processor that loads the configuration of the curator from the configuration
/// document, which is only enabled if it is configured in the curator module, under the name
/// [CuratorModule#CONFIGURATION_DOCUMENT_KEY]. Without a configuration document, the defaults of
/// all settings apply, and no [Extension]s are loaded.
///
/// The configuration is read from the first YAML code block (a code block with language `yaml` or
/// `yml`) in the configuration document; other code blocks and the front matter are ignored. These
/// properties are supported; all of them are optional:
///
/// - `delay`: the number of seconds to wait after the last detected change before running the
/// queries and writing documents to disk; see [DefaultCurator]. It must be a non-negative integer.
/// If it's not, or if it's missing, the default of [CuratorSettings#DEFAULT_DELAY_IN_SECONDS]
/// seconds applies.
/// - `locale`: the locale for translations in query output, as an IETF BCP 47 language tag, like
/// `nl` or `en-GB`. Its language must be a known ISO 639 language. If it's not, or if it's missing,
/// the default of [CuratorSettings#DEFAULT_LOCALE] applies.
/// - `extensions`: a map of extension names to their configuration; an extension without
/// configuration has an empty value.
///
/// ````markdown
/// ```yaml
/// delay: 5
/// locale: nl
/// extensions:
///   directory:
///     teams-folder: Teams
///   another-extension:
/// ```
/// ````
///
/// The configuration is (re)loaded on every [Reset]; that is: at startup, and when the watch
/// document is touched. It is also reloaded whenever the configuration document changes:
///
/// - The `delay` is applied immediately.
/// - The `locale` is applied immediately as well. It changes the output of all queries, so a change
/// of the locale forces a complete refresh; see below.
/// - Extensions are reloaded only if the `extensions` property changed. This also forces a complete
/// refresh.
///
/// For a complete refresh the loader reloads the vault from disk and publishes a [Reset], which
/// triggers a complete rebuild of all state in all change processors, including the new ones, and
/// reruns all queries. This is the same as touching the watch document; see [VaultReloader].
///
/// Extensions that can't be loaded - unknown extensions, invalid settings, missing services, and so
/// on - are left out; see the [ExtensionResolver]. The configuration is only rejected as a whole if
/// it can't be parsed.
///
/// The loader doesn't activate the extensions itself. It stages them in the [ExtensionRegistry];
/// the [DefaultChangeProcessorOrchestrator] activates them.
@Singleton
final class ConfigurationLoader
    implements ChangeProcessor
{
    static final String DELAY_PROPERTY = "delay";
    static final String LOCALE_PROPERTY = "locale";
    static final String EXTENSIONS_PROPERTY = "extensions";

    private static final Logger LOGGER = getLogger(ConfigurationLoader.class);
    private static final Set<String> ISO_LANGUAGES = Set.of(Locale.getISOLanguages());

    private final String configurationDocumentName;
    private final Vault vault;
    private final ExtensionResolver resolver;
    private final ExtensionRegistry registry;
    private final CuratorSettings settings;
    private Object lastExtensionsConfiguration;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    ConfigurationLoader(
        @Named(CONFIGURATION_DOCUMENT_KEY) Optional<String> configurationDocumentName,
        Vault vault,
        ExtensionResolver resolver,
        ExtensionRegistry registry,
        CuratorSettings settings)
    {
        this.configurationDocumentName = configurationDocumentName.orElse(null);
        this.vault = vault;
        this.resolver = resolver;
        this.registry = registry;
        this.settings = settings;
        this.lastExtensionsConfiguration = null;
        if (this.configurationDocumentName != null)
        {
            LOGGER.debug("Configuration document configured: '{}'.",
                this.configurationDocumentName
            );
        }
        else if (!resolver.factories().isEmpty())
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
            LOGGER.debug("Reset detected. (Re)loading the configuration document.");
            var configuration = readConfiguration(vault.findDocument(configurationDocumentName));
            applySettings(configuration);
            loadExtensions(configuration.getProperty(EXTENSIONS_PROPERTY).orElse(null));
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
        var refresh = applySettings(configuration);
        var extensionsConfiguration = configuration.getProperty(EXTENSIONS_PROPERTY).orElse(null);
        if (Objects.equals(extensionsConfiguration, lastExtensionsConfiguration))
        {
            LOGGER.debug("Configuration document changed, but the extensions didn't.");
        }
        else
        {
            LOGGER.info("Configuration of extensions has changed.");
            refresh |= loadExtensions(extensionsConfiguration);
        }
        if (!refresh)
        {
            return emptyChangelog();
        }
        LOGGER.info("Forcing a complete refresh.");
        // Like the VaultReloader, reload the vault before publishing the Reset. The in-memory
        // vault might have drifted from what's on disk, for example because a file system event
        // was missed. Query output is only written if it differs from the output in the in-memory
        // document, so without a reload, stale documents are never repaired.
        vault.reload();
        return changelogFor(RESET);
    }

    /// Reads the configuration from the first YAML code block in the configuration document.
    ///
    /// @return the configuration; empty if there is no configuration document, or if it has no YAML
    /// code block.
    private static Dictionary readConfiguration(Optional<Document> document)
    {
        return document
            .flatMap(ConfigurationLoader::findFirstYamlCodeBlock)
            .map(codeBlock -> yamlDictionary(codeBlock.code()))
            .orElse(emptyDictionary());
    }

    /// Applies the settings of the curator itself. Invalid settings are ignored; their defaults
    /// apply instead.
    ///
    /// @return `true` if a setting changed that requires a complete refresh.
    private boolean applySettings(Dictionary configuration)
    {
        // Read the raw values: the typed accessors of the Dictionary accept a list and return its
        // first element, but a list is not a valid value for these settings.
        int delay = configuration.getProperty(DELAY_PROPERTY)
            .filter(Integer.class::isInstance)
            .map(Integer.class::cast)
            .filter(value -> value >= 0)
            .orElse(DEFAULT_DELAY_IN_SECONDS);
        if (delay != settings.delayInSeconds())
        {
            LOGGER.info("Setting the delay to {} second(s).", delay);
            settings.setDelayInSeconds(delay);
        }
        var locale = parseLocale(configuration.getProperty(LOCALE_PROPERTY)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .orElse(DEFAULT_LOCALE));
        if (!locale.equals(settings.locale()))
        {
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info("Setting the locale to '{}'.", locale.toLanguageTag());
            }
            settings.setLocale(locale);
            return true;
        }
        return false;
    }

    /// Parses the locale from a language tag. Underscores are accepted as separators as well, so
    /// that both `en-GB` and `en_GB` work.
    private static Locale parseLocale(String value)
    {
        var locale = Locale.forLanguageTag(value.strip().replace('_', '-'));
        if (ISO_LANGUAGES.contains(locale.getLanguage()))
        {
            return locale;
        }
        return Locale.forLanguageTag(DEFAULT_LOCALE);
    }

    /// Finds the first YAML code block in the document, in document order, at any section level.
    private static Optional<CodeBlock> findFirstYamlCodeBlock(Document document)
    {
        var finder = new YamlCodeBlockFinder();
        document.accept(finder);
        return Optional.ofNullable(finder.result);
    }

    private static final class YamlCodeBlockFinder
        extends BreadthFirstVaultVisitor
    {
        private CodeBlock result;

        @Override
        public void visit(Section section)
        {
            if (result == null)
            {
                super.visit(section);
            }
        }

        @Override
        public void visit(CodeBlock codeBlock)
        {
            if (result == null && isYaml(codeBlock))
            {
                result = codeBlock;
            }
        }

        private static boolean isYaml(CodeBlock codeBlock)
        {
            var info = codeBlock.language().strip();
            var language = info.split("\\s+", 2)[0];
            return language.equalsIgnoreCase("yaml") || language.equalsIgnoreCase("yml");
        }
    }

    /// Resolves the configuration of the extensions into a new generation of extensions and stages
    /// it in the registry. Extensions that can't be loaded are left out of the generation.
    ///
    /// @return `true` if a generation was staged, `false` if the configuration was rejected as a
    /// whole, because it can't be parsed.
    private boolean loadExtensions(Object configuration)
    {
        lastExtensionsConfiguration = configuration;
        try
        {
            var generation = resolver.resolve(parseExtensionsConfiguration(configuration));
            registry.stage(generation);
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info("Staged {} extension(s): {}.", generation.extensions().size(),
                    String.join(", ", generation.extensions().keySet())
                );
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

    /// Parses the raw configuration of the extensions from the YAML code block into a map of
    /// extension names to their configuration; the map is ordered for predictability.
    private static SortedMap<String, Dictionary> parseExtensionsConfiguration(Object configuration)
    {
        var result = new TreeMap<String, Dictionary>();
        switch (configuration)
        {
            case null ->
            {
                // No extensions configured.
            }
            case Dictionary dictionary -> dictionary.propertyNames().forEach(name ->
                result.put(name, toDictionary(name, dictionary.getProperty(name).orElse(null))));
            default -> throw new IllegalArgumentException(
                "Property '" + EXTENSIONS_PROPERTY + "' must be a map.");
        }
        return result;
    }

    private static Dictionary toDictionary(String extensionName, Object value)
    {
        return switch (value)
        {
            case null -> emptyDictionary();
            case Dictionary dictionary -> dictionary;
            default -> throw new IllegalArgumentException(
                "Configuration of extension '" + extensionName + "' must be a map.");
        };
    }
}

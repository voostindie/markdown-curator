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
import static nl.ulso.dictionary.Dictionary.emptyDictionary;
import static nl.ulso.dictionary.Dictionary.yamlDictionary;
import static org.slf4j.LoggerFactory.getLogger;

/// Special change processor that loads [Extension]s based on the configuration document, which
/// is only enabled if it is configured in the curator module, under the name
/// [CuratorModule#CONFIGURATION_DOCUMENT_KEY].
///
/// The configuration is read from the `extensions` property in the first YAML code block (a code
/// block with language `yaml` or `yml`) in the configuration document; other code blocks and the
/// front matter are ignored. The property is either a list of extension names, or a map of
/// extension names to their configuration:
///
/// ````markdown
/// ```yaml
/// extensions:
///   directory:
///     teams-folder: Teams
///   another-extension:
/// ```
/// ````
///
/// The configuration is (re)loaded:
///
/// - On every [Reset]; that is: at startup, and when the watch document is touched.
/// - Whenever the configuration document changes, but only if the `extensions` property changed.
/// In this case the loader publishes a [Reset], which triggers a complete rebuild of all state in
/// all change processors, including the new ones.
///
/// Extensions that can't be loaded - unknown extensions, invalid settings, missing services, and so
/// on - are left out; see the [ExtensionResolver]. The configuration is only rejected as a whole if
/// it can't be parsed.
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
    private final ExtensionResolver resolver;
    private final ExtensionRegistry registry;
    private Object lastConfiguration;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    ExtensionLoader(
        @Named(CONFIGURATION_DOCUMENT_KEY) Optional<String> configurationDocumentName,
        Vault vault,
        ExtensionResolver resolver,
        ExtensionRegistry registry)
    {
        this.configurationDocumentName = configurationDocumentName.orElse(null);
        this.vault = vault;
        this.resolver = resolver;
        this.registry = registry;
        this.lastConfiguration = null;
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
            .flatMap(ExtensionLoader::findFirstYamlCodeBlock)
            .flatMap(codeBlock -> yamlDictionary(codeBlock.code()).getProperty(EXTENSIONS_PROPERTY))
            .orElse(null);
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

    /// Resolves the configuration into a new generation of extensions and stages it in the
    /// registry. Extensions that can't be loaded are left out of the generation.
    ///
    /// @return `true` if a generation was staged, `false` if the configuration was rejected as a
    /// whole, because it can't be parsed.
    private boolean loadConfiguration(Object configuration)
    {
        lastConfiguration = configuration;
        try
        {
            var generation = resolver.resolve(parseConfiguration(configuration));
            registry.stage(generation);
            if (LOGGER.isInfoEnabled())
            {
                LOGGER.info("Staged {} extension(s): {}.", generation.extensions().size(),
                    String.join(", ", generation.extensions().keySet()));
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

    /// Parses the raw configuration from the YAML code block into a map of extension names to their
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
            case Dictionary dictionary -> dictionary.propertyNames().forEach(name ->
                result.put(name, toDictionary(name, dictionary.getProperty(name).orElse(null))));
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
            case Dictionary dictionary -> dictionary;
            default -> throw new IllegalArgumentException(
                "Configuration of extension '" + extensionName + "' must be a map.");
        };
    }
}

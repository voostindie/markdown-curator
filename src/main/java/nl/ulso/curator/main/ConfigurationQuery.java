package nl.ulso.curator.main;

import jakarta.inject.*;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.change.Reset;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;

import java.util.*;

import static java.lang.System.lineSeparator;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static nl.ulso.curator.CuratorModule.CONFIGURATION_DOCUMENT_KEY;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.main.ConfigurationLoader.DELAY_PROPERTY;
import static nl.ulso.curator.main.ConfigurationLoader.LOCALE_PROPERTY;
import static nl.ulso.curator.main.CuratorSettings.DEFAULT_DELAY_IN_SECONDS;
import static nl.ulso.curator.main.CuratorSettings.DEFAULT_LOCALE;

/// Shows the active configuration of the curator, as loaded from the configuration document by the
/// [ConfigurationLoader]:
///
/// - The settings of the curator itself, like the delay and the locale. The loader ignores invalid
/// settings, so this shows the values that are actually in effect.
/// - All extensions that are available to the curator, whether they are active, and why the last
/// configuration was rejected, if it was.
/// - All queries that are currently available, built-in and from active extensions.
/// - All output formats that are currently available, built-in and from active extensions.
///
/// The [QueryCatalog] is injected through a [Provider] because the catalog itself is built from
/// all queries, including this one.
@Singleton
final class ConfigurationQuery
    implements Query
{
    private final CuratorSettings settings;
    private final ExtensionRegistry registry;
    private final Provider<QueryCatalog> queryCatalogProvider;
    private final DynamicQueryResultFormatterRegistry formatterRegistry;
    private final QueryResultFactory queryResultFactory;
    private final String configurationDocumentName;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    ConfigurationQuery(
        CuratorSettings settings,
        ExtensionRegistry registry,
        Provider<QueryCatalog> queryCatalogProvider,
        DynamicQueryResultFormatterRegistry formatterRegistry,
        QueryResultFactory queryResultFactory,
        @Named(CONFIGURATION_DOCUMENT_KEY) Optional<String> configurationDocumentName)
    {
        this.settings = settings;
        this.registry = registry;
        this.queryCatalogProvider = queryCatalogProvider;
        this.formatterRegistry = formatterRegistry;
        this.queryResultFactory = queryResultFactory;
        this.configurationDocumentName = configurationDocumentName.orElse(null);
    }

    @Override
    public String name()
    {
        return "configuration";
    }

    @Override
    public String description()
    {
        return "Shows the curator settings, all available extensions with their status and " +
               "dependencies, and all available queries and output formats.";
    }

    @Override
    public Map<String, String> supportedConfiguration()
    {
        return Map.of();
    }

    @Override
    public boolean isImpactedBy(Changelog changelog, QueryDefinition definition)
    {
        return changelog.changes().anyMatch(isPayloadType(Reset.class)) ||
               (configurationDocumentName != null &&
                changelog.changesFor(Document.class).anyMatch(change ->
                    change.value().name().equals(configurationDocumentName)));
    }

    @Override
    public QueryResult run(QueryDefinition definition)
    {
        var builder = new StringBuilder();
        if (configurationDocumentName == null)
        {
            builder.append("No configuration document is configured; the default settings apply, ")
                .append("and extensions can't be loaded.")
                .append(lineSeparator())
                .append(lineSeparator());
        }
        appendSettings(builder);
        builder.append(lineSeparator());
        appendExtensions(builder);
        builder.append(lineSeparator());
        appendQueries(builder);
        builder.append(lineSeparator());
        appendOutputFormats(builder);
        return queryResultFactory.string(builder.toString());
    }

    private void appendSettings(StringBuilder builder)
    {
        builder.append("### Settings").append(lineSeparator()).append(lineSeparator());
        var delay = settings.delayInSeconds();
        appendSetting(builder, DELAY_PROPERTY,
            delay + (delay == 1 ? " second" : " seconds"),
            delay == DEFAULT_DELAY_IN_SECONDS
        );
        var locale = settings.locale();
        appendSetting(builder, LOCALE_PROPERTY,
            locale.toLanguageTag() + " (" + locale.getDisplayName(Locale.ENGLISH) + ")",
            locale.equals(Locale.forLanguageTag(DEFAULT_LOCALE))
        );
    }

    private static void appendSetting(
        StringBuilder builder, String name, String value, boolean isDefault)
    {
        builder.append("- **").append(name).append("**: ").append(value);
        if (isDefault)
        {
            builder.append(" (default)");
        }
        builder.append(lineSeparator());
    }

    private void appendExtensions(StringBuilder builder)
    {
        builder.append("### Extensions").append(lineSeparator()).append(lineSeparator());
        var status = registry.status();
        var names = new TreeSet<>(status.available().keySet());
        names.addAll(status.failures().keySet());
        if (names.isEmpty())
        {
            builder.append("No extensions are available.").append(lineSeparator());
        }
        for (var name : names)
        {
            var failure = status.failures().get(name);
            builder.append("- ")
                .append(failure != null ? "🔴 " : status.active().contains(name) ? "🟢 " : "⚪️️")
                .append("**").append(name).append("**");
            var factory = status.available().get(name);
            if (factory != null && !factory.description().isBlank())
            {
                builder.append(": ").append(factory.description());
            }
            builder.append(lineSeparator());
            if (failure != null)
            {
                appendDetail(builder, "not loaded", failure);
            }
            if (factory != null)
            {
                appendDetail(builder, "requires", typeNames(factory.requires()));
                appendDetail(builder, "provides", typeNames(factory.provides()));
                appendExtensionSettings(builder, factory.supportedConfiguration());
            }
        }
        status.lastError().ifPresent(error -> builder.append(lineSeparator())
            .append("**The last configuration was rejected**: ")
            .append(error)
            .append(lineSeparator()));
    }

    private void appendQueries(StringBuilder builder)
    {
        builder.append("### Queries").append(lineSeparator()).append(lineSeparator());
        var queries = queryCatalogProvider.get().queries();
        if (queries.isEmpty())
        {
            builder.append("No queries are available.").append(lineSeparator());
            return;
        }
        queries.stream().sorted(comparing(Query::name)).forEach(query -> {
            builder.append("- **").append(query.name()).append("**");
            var description = query.description();
            if (description != null && !description.isBlank())
            {
                builder.append(": ").append(description.strip());
            }
            builder.append(lineSeparator());
        });
    }

    /// Lists all output formats, with their MIME type, whether they're embedded inline or in a
    /// fenced code block, and where they come from.
    private void appendOutputFormats(StringBuilder builder)
    {
        builder.append("### Output formats").append(lineSeparator()).append(lineSeparator());
        var staticFormats = formatterRegistry.staticOutputFormats();
        var providers = registry.status().outputFormatProviders();
        formatterRegistry.outputFormats().stream()
            .sorted(comparing(OutputFormat::name))
            .forEach(format -> {
                builder.append("- **").append(format.name()).append("**: `")
                    .append(format.mimeType()).append("`, ")
                    .append(format.inline() ? "inline" : "fenced");
                if (!staticFormats.contains(format))
                {
                    var names = providers.get(format.name());
                    if (names != null && !names.isEmpty())
                    {
                        builder.append(", from ").append(String.join(", ", names));
                    }
                }
                builder.append(lineSeparator());
            });
    }

    private static void appendDetail(StringBuilder builder, String label, String value)
    {
        if (!value.isBlank())
        {
            builder.append("    - ").append(label).append(": ").append(value).append(lineSeparator());
        }
    }

    /// Lists the settings an extension supports, one per line, sorted by name, each with its
    /// description, if it has one.
    private static void appendExtensionSettings(
        StringBuilder builder, Map<String, String> supportedConfiguration)
    {
        if (supportedConfiguration.isEmpty())
        {
            return;
        }
        builder.append("    - settings:").append(lineSeparator());
        new TreeMap<>(supportedConfiguration).forEach((name, description) -> {
            builder.append("        - `").append(name).append("`");
            if (description != null && !description.isBlank())
            {
                builder.append(": ").append(description.strip());
            }
            builder.append(lineSeparator());
        });
    }

    private static String typeNames(Set<Class<?>> types)
    {
        return types.stream().map(Class::getSimpleName).sorted().collect(joining(", "));
    }
}

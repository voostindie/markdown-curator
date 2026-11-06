package nl.ulso.curator.main;

import jakarta.inject.*;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.change.Reset;
import nl.ulso.curator.query.*;
import nl.ulso.curator.vault.Document;

import java.util.*;

import static java.lang.System.lineSeparator;
import static nl.ulso.curator.CuratorModule.CONFIGURATION_DOCUMENT_KEY;
import static nl.ulso.curator.change.Change.isPayloadType;

/// Lists all extensions that are available to the curator, whether they are active, and why the
/// last configuration was rejected, if it was.
@Singleton
final class ExtensionsQuery
    implements Query
{
    private final ExtensionRegistry registry;
    private final QueryResultFactory queryResultFactory;
    private final String configurationDocumentName;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    ExtensionsQuery(
        ExtensionRegistry registry,
        QueryResultFactory queryResultFactory,
        @Named(CONFIGURATION_DOCUMENT_KEY) Optional<String> configurationDocumentName)
    {
        this.registry = registry;
        this.queryResultFactory = queryResultFactory;
        this.configurationDocumentName = configurationDocumentName.orElse(null);
    }

    @Override
    public String name()
    {
        return "extensions";
    }

    @Override
    public String description()
    {
        return "Lists all available extensions and their status.";
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
        var status = registry.status();
        var builder = new StringBuilder();
        if (configurationDocumentName == null)
        {
            builder.append("No configuration document is configured; extensions can't be loaded.")
                .append(lineSeparator());
        }
        if (status.available().isEmpty())
        {
            builder.append("No extensions are available.").append(lineSeparator());
        }
        else
        {
            status.available().forEach(name -> builder.append("- ")
                .append(status.active().contains(name) ? "✅ " : "⬜️ ")
                .append(name)
                .append(lineSeparator()));
        }
        status.lastError().ifPresent(error -> builder.append(lineSeparator())
            .append("**The last configuration was rejected**: ")
            .append(error)
            .append(lineSeparator()));
        return queryResultFactory.string(builder.toString());
    }
}

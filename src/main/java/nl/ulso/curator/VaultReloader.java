package nl.ulso.curator;

import jakarta.inject.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static nl.ulso.curator.Change.isUpdate;
import static nl.ulso.curator.Change.update;

/// Special change processor that is only enabled if a watch document is configured in the curator
/// module, under the name [#WATCH_DOCUMENT_KEY].
///
/// This processor monitors changes to the watch document. If it is changed on disk, it publishes a
/// [Vault] update, which change processors can consume to perform a full refresh of their internal
/// data structures. The [Vault] update also triggers the execution of all queries, regardless of
/// the actual changes that were detected.
///
/// This provides an easy way to force a complete reload and refresh without having to restart the
/// application. It's just "touch and go", but in a good way.
@Singleton
public final class VaultReloader
    extends ChangeProcessorTemplate
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultReloader.class);

    public static final String WATCH_DOCUMENT_KEY = "watchdoc";

    private final Vault vault;
    private final String watchDocumentName;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    VaultReloader(Vault vault, @Named(WATCH_DOCUMENT_KEY) Optional<String> watchDocumentName)
    {
        this.vault = vault;
        this.watchDocumentName = watchDocumentName.orElse(null);
        if (LOGGER.isDebugEnabled() && watchDocumentName.isPresent())
        {
            LOGGER.debug("Watch document configured: {}.", watchDocumentName);
        }
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Document.class);
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(Vault.class);
    }

    @Override
    protected boolean isFullRefreshRequired(Changelog changelog)
    {
        if (watchDocumentName == null)
        {
            return false;
        }
        return changelog.changes().anyMatch(isUpdate().and(change ->
                change.as(Document.class).value().name().equals(watchDocumentName))
        );
    }

    @Override
    protected Collection<Change<?>> fullRefresh()
    {
        LOGGER.info("Watch document has changed. Forcing a complete refresh.");
        return List.of(update(vault, Vault.class));
    }
}

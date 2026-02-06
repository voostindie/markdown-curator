package nl.ulso.curator.main;

import jakarta.inject.*;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.change.ChangeProcessor;
import nl.ulso.curator.change.Changelog;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

import static nl.ulso.curator.change.Change.isUpdate;
import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.change.Changelog.emptyChangelog;

/// Special change processor that is only enabled if a watch document is configured in the curator
/// module, under the name [CuratorModule#WATCH_DOCUMENT_KEY].
///
/// This processor monitors changes to the watch document. If it is changed on disk, it publishes a
/// [Vault] update, which change processors can consume to perform a full refresh of their internal
/// data structures. The [Vault] update also triggers the execution of all queries, regardless of
/// the actual changes that were detected.
///
/// This provides an easy way to force a complete reload and refresh without having to restart the
/// application. It's just "touch and go", but in a good way.
@Singleton
final class VaultReloader
    implements ChangeProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultReloader.class);

    private final Vault vault;
    private final String watchDocumentName;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    VaultReloader(Vault vault, @Named(CuratorModule.WATCH_DOCUMENT_KEY) Optional<String> watchDocumentName)
    {
        this.vault = vault;
        this.watchDocumentName = watchDocumentName.orElse(null);
        if (LOGGER.isDebugEnabled())
        {
            if (this.watchDocumentName != null)
            {
                LOGGER.debug("Watch document configured: {}.", watchDocumentName);
            }
            else
            {
                LOGGER.debug(
                    "No watch document configured. The only way to run all queries is to restart " +
                    "the application.");
            }
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
    public Changelog apply(Changelog changelog)
    {
        if (watchDocumentName != null &&
            changelog.changes().anyMatch(isUpdate().and(change ->
                change.as(Document.class).value().name().equals(watchDocumentName))))
        {
            LOGGER.info("Watch document has changed. Forcing a complete refresh.");
            return changelogFor(update(vault, Vault.class));
        }
        return emptyChangelog();
    }
}

package nl.ulso.curator.main;

import jakarta.inject.*;
import nl.ulso.curator.CuratorModule;
import nl.ulso.curator.change.*;
import nl.ulso.curator.vault.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

import static nl.ulso.curator.CuratorModule.WATCH_DOCUMENT_KEY;
import static nl.ulso.curator.change.Change.isUpdate;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.change.Changelog.emptyChangelog;
import static nl.ulso.curator.change.Reset.RESET;

/// Special change processor that is only enabled if a watch document is configured in the curator
/// module, under the name [CuratorModule#WATCH_DOCUMENT_KEY].
///
/// This processor monitors changes to the watch document. If it is changed on disk, it publishes a
/// [Reset] change, which in turn triggers all change processors to be reset first. Next, only the
/// changes after the last [Reset] change in the changelog are applied to the change processors.
///
/// This provides an easy way to force a complete reload and refresh without having to restart the
/// application. It's just "touch and go".
@Singleton
final class VaultReloader
    implements ChangeProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultReloader.class);

    private final String watchDocumentName;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    VaultReloader(@Named(WATCH_DOCUMENT_KEY) Optional<String> watchDocumentName)
    {
        this.watchDocumentName = watchDocumentName.orElse(null);
        if (LOGGER.isDebugEnabled())
        {
            if (this.watchDocumentName != null)
            {
                LOGGER.debug("Watch document configured: '{}'.", this.watchDocumentName);
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
        return Set.of(Reset.class);
    }

    @Override
    public Changelog apply(Changelog changelog)
    {
        if (watchDocumentName != null &&
            changelog.changes().anyMatch(isUpdate().and(change ->
                change.as(Document.class).value().name().equals(watchDocumentName))))
        {
            LOGGER.info("Watch document has changed. Forcing a complete refresh.");
            return changelogFor(RESET);
        }
        return emptyChangelog();
    }
}

package nl.ulso.markdown_curator;

import jakarta.inject.*;
import nl.ulso.markdown_curator.vault.Document;
import nl.ulso.markdown_curator.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static nl.ulso.markdown_curator.Change.isObjectType;
import static nl.ulso.markdown_curator.Change.isUpdate;
import static nl.ulso.markdown_curator.Change.update;

/// Special change processor that is only enabled if a watch document is configured in the module,
/// under the name [#WATCH_DOCUMENT_KEY].
///
/// This processor monitors changes to the watch document. If it is updated it publishes a [Vault]
/// update, which triggers a full refresh of all change processors, as well as the running of all
/// queries in the vault, independent of the actual changes that were detected.
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
    public Set<Class<?>> consumedObjectTypes()
    {
        return Set.of(Document.class);
    }

    @Override
    public Set<Class<?>> producedObjectTypes()
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
        return changelog.changes().anyMatch(
            isObjectType(Document.class).and(isUpdate()).and(change ->
            {
                LOGGER.debug("Watch document has changed. Forcing a complete refresh.");
                var document = change.objectAs(Document.class);
                return document.name().equals(watchDocumentName);
            })
        );
    }

    @Override
    protected Collection<Change<?>> fullRefresh()
    {
        return List.of(update(vault, Vault.class));
    }
}

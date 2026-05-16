package nl.ulso.curator.main;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;
import nl.ulso.curator.vault.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static nl.ulso.curator.change.Changelog.changelogFor;

/// Special change processor that triggers on [Vault] events and then generates a CREATE change for
/// every folder and document in the vault.
///
/// This processor can be triggered in one of two ways:
/// 1. On startup, when the system publishes an initial [Vault] CREATE chaneg.
/// 2. On demand by the [VaultReloader], when the watch document is changed.
@Singleton
final class VaultInitializer
    implements ChangeProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultInitializer.class);

    private final Vault vault;

    @Inject
    VaultInitializer(Vault vault)
    {
        this.vault = vault;
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Vault.class);
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(Document.class, Folder.class);
    }

    @Override
    public Changelog apply(Changelog changelog)
    {
        LOGGER.debug(
            "Initializing the vault by publishing CREATE events for every folder and document.");
        var finder = new FolderAndDocumentChangeFinder();
        vault.accept(finder);
        LOGGER.info("Initialized the vault with {} changes.", finder.changes.size());
        return changelogFor(finder.changes);
    }

    private static final class FolderAndDocumentChangeFinder
        extends BreadthFirstVaultVisitor
    {
        private final List<Change<?>> changes = new ArrayList<>();

        @Override
        public void visit(Folder folder)
        {
            changes.add(Change.create(folder, Folder.class));
            super.visit(folder);
        }

        @Override
        public void visit(Document document)
        {
            changes.add(Change.create(document, Document.class));
        }
    }
}

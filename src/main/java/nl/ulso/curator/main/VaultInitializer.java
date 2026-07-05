package nl.ulso.curator.main;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;
import nl.ulso.curator.vault.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static nl.ulso.curator.change.ChangeCollector.newChangeCollector;

/// Special change processor that triggers on [Reset] events and then generates a CREATE change for
/// every folder and document in the vault.
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
        return Set.of(Reset.class);
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
        var newChangelog = finder.collector.changelog();
        LOGGER.info("Initialized the vault with {} changes.", newChangelog.size());
        return newChangelog;
    }

    private static final class FolderAndDocumentChangeFinder
        extends BreadthFirstVaultVisitor
    {
        private final ChangeCollector collector = newChangeCollector();

        @Override
        public void visit(Folder folder)
        {
            collector.create(folder, Folder.class);
            super.visit(folder);
        }

        @Override
        public void visit(Document document)
        {
            collector.create(document, Document.class);
        }
    }
}

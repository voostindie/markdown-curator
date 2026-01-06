package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.vault.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static nl.ulso.markdown_curator.Change.update;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static nl.ulso.markdown_curator.Changelog.emptyChangelog;
import static org.assertj.core.api.Assertions.assertThat;

class VaultReloaderTest
{
    private VaultStub vault;

    @BeforeEach
    void setUp()
    {
        this.vault = new VaultStub();
    }

    @Test
    void consumedObjectTypes()
    {
        var set = new VaultReloader(vault, Optional.empty()).consumedObjectTypes();
        assertThat(set).containsExactly(Document.class);
    }

    @Test
    void producedObjectTypes()
    {
        var set = new VaultReloader(vault, Optional.empty()).producedObjectTypes();
        assertThat(set).containsExactly(Vault.class);
    }

    @Test
    void doNothingIfNoWatchDocIsProvided()
    {
        var reloader = new VaultReloader(vault, Optional.empty());
        assertThat(reloader.isFullRefreshRequired(emptyChangelog())).isFalse();
    }

    @Test
    void triggerFullRefreshIfWatchDocChanges()
    {
        var document = vault.addDocumentInPath("WATCHER", "");
        var reloader = new VaultReloader(vault, Optional.of("WATCHER"));
        var changelog = changelogFor(update(document, Document.class));
        assertThat(reloader.isFullRefreshRequired(changelog)).isTrue();
    }

    @Test
    void fullRefreshResultsInVaultChangeEvent()
    {
        var document = vault.addDocumentInPath("WATCHER", "");
        var reloader = new VaultReloader(vault, Optional.of("WATCHER"));
        var inputChangelog = changelogFor(update(document, Document.class));
        var outputChangelog = reloader.run(inputChangelog);
        assertThat(outputChangelog.changes()
            .allMatch(change -> change.objectType() == Vault.class)).isTrue();
    }
}

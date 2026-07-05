package nl.ulso.curator.main;

import nl.ulso.curator.change.Reset;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.VaultStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static nl.ulso.curator.change.Changelog.emptyChangelog;
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
    void consumedPayloadTypes()
    {
        var set = new VaultReloader(Optional.empty(), vault).consumedPayloadTypes();
        assertThat(set).containsExactly(Document.class);
    }

    @Test
    void producedPayloadTypes()
    {
        var set = new VaultReloader(Optional.empty(), vault).producedPayloadTypes();
        assertThat(set).containsExactly(Reset.class);
    }

    @Test
    void doNothingIfNoWatchDocIsProvided()
    {
        var reloader = new VaultReloader(Optional.empty(), vault);
        reloader.apply(emptyChangelog());
        assertThat(vault.reloadCount()).isZero();
    }

    @Test
    void triggerFullRefreshIfWatchDocChanges()
    {
        var document = vault.addDocumentInPath("WATCHER", "");
        var reloader = new VaultReloader(Optional.of("WATCHER"), vault);
        var changelog = changelogFor(update(document, Document.class));
        reloader.apply(changelog);
        assertThat(vault.reloadCount()).isPositive();
    }

    @Test
    void fullRefreshResultsInVaultChangeEvent()
    {
        var document = vault.addDocumentInPath("WATCHER", "");
        var reloader = new VaultReloader(Optional.of("WATCHER"), vault);
        var inputChangelog = changelogFor(update(document, Document.class));
        var outputChangelog = reloader.apply(inputChangelog);
        assertThat(outputChangelog.changes()
            .allMatch(change -> change.payloadType() == Reset.class)).isTrue();
    }
}

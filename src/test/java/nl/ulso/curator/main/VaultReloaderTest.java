package nl.ulso.curator.main;

import nl.ulso.curator.vault.*;
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
        var set = new VaultReloader(vault, Optional.empty()).consumedPayloadTypes();
        assertThat(set).containsExactly(Document.class);
    }

    @Test
    void producedPayloadTypes()
    {
        var set = new VaultReloader(vault, Optional.empty()).producedPayloadTypes();
        assertThat(set).containsExactly(Vault.class);
    }

    @Test
    void doNothingIfNoWatchDocIsProvided()
    {
        var reloader = new VaultReloader(vault, Optional.empty());
        assertThat(reloader.apply(emptyChangelog())).isEqualTo(emptyChangelog());
    }

    @Test
    void triggerFullRefreshIfWatchDocChanges()
    {
        var document = vault.addDocumentInPath("WATCHER", "");
        var reloader = new VaultReloader(vault, Optional.of("WATCHER"));
        var changelog = changelogFor(update(document, Document.class));
        assertThat(reloader.apply(changelog).isEmpty()).isFalse();
    }

    @Test
    void fullRefreshResultsInVaultChangeEvent()
    {
        var document = vault.addDocumentInPath("WATCHER", "");
        var reloader = new VaultReloader(vault, Optional.of("WATCHER"));
        var inputChangelog = changelogFor(update(document, Document.class));
        var outputChangelog = reloader.apply(inputChangelog);
        assertThat(outputChangelog.changes()
            .allMatch(change -> change.payloadType() == Vault.class)).isTrue();
    }
}

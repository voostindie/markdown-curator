package nl.ulso.curator.main;

import nl.ulso.curator.vault.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static nl.ulso.curator.changelog.Change.update;
import static nl.ulso.curator.changelog.Changelog.changelogFor;
import static nl.ulso.curator.changelog.Changelog.emptyChangelog;
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
        assertThat(reloader.run(emptyChangelog())).isEqualTo(emptyChangelog());
    }

    @Test
    void triggerFullRefreshIfWatchDocChanges()
    {
        var document = vault.addDocumentInPath("WATCHER", "");
        var reloader = new VaultReloader(vault, Optional.of("WATCHER"));
        var changelog = changelogFor(update(document, Document.class));
        assertThat(reloader.run(changelog).isEmpty()).isFalse();
    }

    @Test
    void fullRefreshResultsInVaultChangeEvent()
    {
        var document = vault.addDocumentInPath("WATCHER", "");
        var reloader = new VaultReloader(vault, Optional.of("WATCHER"));
        var inputChangelog = changelogFor(update(document, Document.class));
        var outputChangelog = reloader.run(inputChangelog);
        assertThat(outputChangelog.changes()
            .allMatch(change -> change.payloadType() == Vault.class)).isTrue();
    }
}

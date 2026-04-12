package nl.ulso.curator.main;

import nl.ulso.curator.vault.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Changelog.changelogFor;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class VaultInitializerTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    private VaultStub vault;

    @BeforeEach
    void setUp()
    {
        this.vault = new VaultStub();
    }

    @Test
    void consumedPayloadTypes()
    {
        var set = new VaultInitializer(vault).consumedPayloadTypes();
        assertThat(set).containsExactly(Vault.class);
    }

    @Test
    void producedPayloadTypes()
    {
        var set = new VaultInitializer(vault).producedPayloadTypes();
        assertThat(set).containsExactlyInAnyOrder(Folder.class, Document.class);
    }

    @Test
    void oneEventPerFolderAndDocument()
    {
        vault.addDocumentInPath("Folder 1/Document 1", "");
        vault.addDocumentInPath("Folder 1/Document 2", "");
        vault.addDocumentInPath("Folder 1/Folder 2/Document 3", "");
        vault.addDocumentInPath("Folder 3/Document 4", "");
        var initializer = new VaultInitializer(vault);
        var inputChangelog = changelogFor(create(vault, Vault.class));
        var outputChangelog = initializer.apply(inputChangelog);
        softly.assertThat(outputChangelog.changes().count()).isEqualTo(7);
        softly.assertThat(outputChangelog.changesFor(Folder.class).count()).isEqualTo(3);
        softly.assertThat(outputChangelog.changesFor(Document.class).count()).isEqualTo(4);
    }
}

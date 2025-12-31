package nl.ulso.markdown_curator.links;

import nl.ulso.markdown_curator.Changelog;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static nl.ulso.markdown_curator.Changelog.emptyChangelog;
import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.*;
import static org.assertj.core.api.Assertions.assertThat;

class LinksModelTest
{
    private VaultStub vault;
    private LinksModel model;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        vault.addDocumentInPath("a/foo", "[[bar]] and [[baz]]");
        vault.addDocumentInPath("b/bar", "");
        model = new LinksModel(vault);
        model.vaultChanged(vaultRefreshed(), emptyChangelog());
    }

    @Test
    void barHasNoDeadLinks()
    {
        assertThat(model.deadLinksFor("bar")).isEmpty();
    }

    @Test
    void fooHasDeadLinks()
    {
        assertThat(model.deadLinksFor("foo")).hasSize(1);
    }

    @Test
    void newDocumentBazFixesDeadLinks()
    {
        var document = vault.addDocumentInPath("baz", "");
        model.vaultChanged(documentAdded(document), emptyChangelog());
        assertThat(model.deadLinksFor("foo")).isEmpty();
    }

    @Test
    void updatedDocumentFixedDeadLinks()
    {
        var document = vault.addDocumentInPath("a/foo", "[[bar]]");
        model.vaultChanged(documentChanged(document), emptyChangelog());
        assertThat(model.deadLinksFor("foo")).isEmpty();
    }

    @Test
    void removedDocumentBarAddsDeadLinks()
    {
        var bar = vault.resolveDocumentInPath("b/bar");
        model.vaultChanged(documentRemoved(bar), emptyChangelog());
        assertThat(model.deadLinksFor("foo")).hasSize(2);
    }

    @Test
    void newFolderFixesDeadLinks()
    {
        vault.addDocumentInPath("folder/baz", "");
        model.vaultChanged(folderAdded(vault.folder("folder").orElseThrow()), emptyChangelog());
        assertThat(model.deadLinksFor("foo")).isEmpty();
    }

    @Test
    void deletedFolderAddsDeadLinks()
    {
        model.vaultChanged(folderRemoved(vault.folder("b").orElseThrow()), emptyChangelog());
        assertThat(model.deadLinksFor("foo")).hasSize(2);
    }
}

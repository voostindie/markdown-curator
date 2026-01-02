package nl.ulso.markdown_curator.links;

import nl.ulso.markdown_curator.vault.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static nl.ulso.markdown_curator.Change.create;
import static nl.ulso.markdown_curator.Change.delete;
import static nl.ulso.markdown_curator.Change.update;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
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
        model.fullRefresh();
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
        model.run(changelogFor(create(document, Document.class)));
        assertThat(model.deadLinksFor("foo")).isEmpty();
    }

    @Test
    void updatedDocumentFixedDeadLinks()
    {
        var document = vault.addDocumentInPath("a/foo", "[[bar]]");
        model.run(changelogFor(update(document, Document.class)));
        assertThat(model.deadLinksFor("foo")).isEmpty();
    }

    @Test
    void removedDocumentBarAddsDeadLinks()
    {
        var bar = vault.resolveDocumentInPath("b/bar");
        model.run(changelogFor(delete(bar, Document.class)));
        assertThat(model.deadLinksFor("foo")).hasSize(2);
    }

    @Test
    void newFolderFixesDeadLinks()
    {
        vault.addDocumentInPath("folder/baz", "");
        model.run(changelogFor(create(vault.folder("folder").orElseThrow(), Folder.class)));
        assertThat(model.deadLinksFor("foo")).isEmpty();
    }

    @Test
    void deletedFolderAddsDeadLinks()
    {
        model.run(changelogFor(delete(vault.folder("b").orElseThrow(), Folder.class)));
        assertThat(model.deadLinksFor("foo")).hasSize(2);
    }
}

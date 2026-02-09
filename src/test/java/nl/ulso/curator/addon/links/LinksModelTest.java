package nl.ulso.curator.addon.links;

import nl.ulso.curator.vault.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.ChangeCollector.newChangeCollector;
import static nl.ulso.curator.change.Changelog.changelogFor;
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
        model.reset(newChangeCollector());
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
        model.apply(changelogFor(create(document, Document.class)));
        assertThat(model.deadLinksFor("foo")).isEmpty();
    }

    @Test
    void updatedDocumentFixedDeadLinks()
    {
        var document = vault.addDocumentInPath("a/foo", "[[bar]]");
        model.apply(changelogFor(update(document, Document.class)));
        assertThat(model.deadLinksFor("foo")).isEmpty();
    }

    @Test
    void removedDocumentBarAddsDeadLinks()
    {
        var bar = vault.resolveDocumentInPath("b/bar");
        model.apply(changelogFor(delete(bar, Document.class)));
        assertThat(model.deadLinksFor("foo")).hasSize(2);
    }

    @Test
    void newFolderFixesDeadLinks()
    {
        vault.addDocumentInPath("folder/baz", "");
        model.apply(changelogFor(create(vault.folder("folder").orElseThrow(), Folder.class)));
        assertThat(model.deadLinksFor("foo")).isEmpty();
    }

    @Test
    void deletedFolderAddsDeadLinks()
    {
        model.apply(changelogFor(delete(vault.folder("b").orElseThrow(), Folder.class)));
        assertThat(model.deadLinksFor("foo")).hasSize(2);
    }
}

package nl.ulso.obsidian.watcher.vault;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class FolderTest
{
    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void equalsContract()
    {
        EqualsVerifier.forClass(Folder.class)
                .withPrefabValues(Folder.class, new Folder("red"), new Folder("blue"))
                .verify();
    }

    @Test
    void rootFolder()
    {
        var root = new Folder("root");
        softly.assertThat(root.name()).isEqualTo("root");
        softly.assertThatThrownBy(root::parent).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void subFolder()
    {
        var root = new Folder("root");
        var sub = root.addFolder("sub");
        softly.assertThat(sub.parent()).isSameAs(root);
        softly.assertThat(root.folders().size()).isEqualTo(1);
        softly.assertThat(root.folder("sub").get()).isSameAs(sub);
    }

    @Test
    void subFolderUniqueNameNewReplacesOld()
    {
        var root = new Folder("root");
        root.addFolder("sub");
        var sub = root.addFolder("sub");
        softly.assertThat(root.folders().size()).isEqualTo(1);
        softly.assertThat(root.folder("sub").get()).isSameAs(sub);
    }

    @Test
    void removeFolder()
    {
        var root = new Folder("root");
        root.addFolder("sub");
        root.removeFolder("sub");
        assertThat(root.folders()).isEmpty();
    }

    @Test
    void document()
    {
        var root = new Folder("root");
        Document document = Document.newDocument("empty", Collections.emptyList());
        root.addDocument(document);
        softly.assertThat(root.documents().size()).isEqualTo(1);
        softly.assertThat(root.document("empty").get()).isSameAs(document);
    }

    @Test
    void documentUniqueNameNewReplacesOld()
    {
        var root = new Folder("root");
        root.addDocument(Document.newDocument("empty", Collections.emptyList()));
        Document document = Document.newDocument("empty", Collections.emptyList());
        root.addDocument(document);
        softly.assertThat(root.documents().size()).isEqualTo(1);
        softly.assertThat(root.document("empty").get()).isSameAs(document);
    }

    @Test
    void removeDocument()
    {
        var root = new Folder("root");
        root.addDocument(Document.newDocument("empty", Collections.emptyList()));
        root.removeDocument("empty");
        assertThat(root.documents()).isEmpty();
    }

    @Test
    void visitor()
    {
        var counter = new ElementCounter();
        new Folder("root").accept(counter);
        assertThat(counter.folders).isEqualTo(1);
    }
}
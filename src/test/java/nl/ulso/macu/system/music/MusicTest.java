package nl.ulso.macu.system.music;

import nl.ulso.macu.query.*;
import nl.ulso.macu.vault.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static nl.ulso.macu.vault.QueryBlockTest.emptyQueryBlock;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class MusicTest
{
    private static Music music;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeAll
    static void constructSystem()
            throws IOException
    {
        music = new Music();
    }

    @Test
    void statistics()
    {
        var vault = music.vault();
        assertThat(vault.name()).endsWith("music");
        var statistics = ElementCounter.countAll(vault);
        System.out.println(statistics);
        softly.assertThat(statistics.vaults).isEqualTo(1);
        softly.assertThat(statistics.folders).isEqualTo(3);
        softly.assertThat(statistics.documents).isEqualTo(11);
        softly.assertThat(statistics.frontMatters).isEqualTo(11);
        softly.assertThat(statistics.sections).isEqualTo(29);
        softly.assertThat(statistics.queries).isEqualTo(4);
        softly.assertThat(statistics.codeBlocks).isEqualTo(5);
        softly.assertThat(statistics.texts).isEqualTo(34);
    }

    @Test
    void queryCatalog()
    {
        QueryCatalog catalog = music.queryCatalog();
        softly.assertThat(catalog.queries().size()).isEqualTo(4);
        Query dummy = catalog.query("dummy");
        QueryResult result = dummy.run(emptyQueryBlock());
        softly.assertThat(result.isSuccess()).isFalse();
        softly.assertThat(result.errorMessage()).contains("no query defined called 'dummy'");
        softly.assertThat(result.errorMessage()).contains("albums");
        softly.assertThat(result.errorMessage()).contains("recordings");
        softly.assertThat(result.errorMessage()).contains("members");
    }

    @Test
    void queries()
    {
        var queries = music.vault().findAllQueryBlocks();
        softly.assertThat(queries.size()).isEqualTo(4);
    }

    @Test
    void internalObjectReferences()
    {
        var visitor = new BreadthFirstVaultVisitor()
        {
            private Folder currentFolder;
            private Document currentDocument;

            @Override
            public void visit(Vault vault)
            {
                currentFolder = vault;
                super.visit(vault);
                currentFolder = null;
            }

            @Override
            public void visit(Folder folder)
            {
                currentFolder = folder;
                super.visit(folder);
                currentFolder = folder.parent();
            }

            @Override
            public void visit(Document document)
            {
                softly.assertThat(document.folder()).isSameAs(currentFolder);
                currentDocument = document;
                super.visit(document);
                currentDocument = null;
            }

            @Override
            public void visit(FrontMatter frontMatter)
            {
                assertDocumentReference(frontMatter);
            }

            @Override
            public void visit(Section section)
            {
                assertDocumentReference(section);
                super.visit(section);
            }

            @Override
            public void visit(CodeBlock codeBlock)
            {
                assertDocumentReference(codeBlock);
            }

            @Override
            public void visit(QueryBlock queryBlock)
            {
                assertDocumentReference(queryBlock);
            }

            @Override
            public void visit(TextBlock textBlock)
            {
                assertDocumentReference(textBlock);
            }

            private void assertDocumentReference(Fragment fragment)
            {
                softly.assertThat(fragment.document()).isSameAs(currentDocument);
            }
        };
        music.vault().accept(visitor);
    }

    @Test
    void runOnce()
    {
        music.runOnce();
    }
}

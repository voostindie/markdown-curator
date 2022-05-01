package nl.ulso.macu.curator;

import nl.ulso.macu.query.*;
import nl.ulso.macu.vault.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Map;

import static nl.ulso.macu.vault.QueryBlockTest.emptyQueryBlock;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class MusicCuratorTest
{
    private MusicCurator musicCurator;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeEach
    void constructSystem()
    {
        musicCurator = new MusicCurator();
    }

    @Test
    void statistics()
    {
        var vault = musicCurator.vault();
        assertThat(vault.name()).endsWith("music");
        var statistics = ElementCounter.countAll(vault);
        System.out.println(statistics);
        softly.assertThat(statistics.vaults).isEqualTo(1);
        softly.assertThat(statistics.folders).isEqualTo(3);
        softly.assertThat(statistics.documents).isEqualTo(13);
        softly.assertThat(statistics.frontMatters).isEqualTo(13);
        softly.assertThat(statistics.sections).isEqualTo(35);
        softly.assertThat(statistics.queries).isEqualTo(10);
        softly.assertThat(statistics.codeBlocks).isEqualTo(5);
        softly.assertThat(statistics.texts).isEqualTo(46);
    }

    @Test
    void queryCatalog()
    {
        QueryCatalog catalog = musicCurator.queryCatalog();
        softly.assertThat(catalog.queries().size()).isEqualTo(4);
        Query dummy = catalog.query("dummy");
        QueryResult result = dummy.run(emptyQueryBlock());
        softly.assertThat(result.isSuccess()).isFalse();
        var markdown = result.toMarkdown();
        softly.assertThat(markdown).contains("no query defined called 'dummy'");
        softly.assertThat(markdown).contains("albums");
        softly.assertThat(markdown).contains("recordings");
        softly.assertThat(markdown).contains("members");
    }

    @Test
    void queries()
    {
        var queries = musicCurator.vault().findAllQueryBlocks();
        softly.assertThat(queries.size()).isEqualTo(10);
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
        musicCurator.vault().accept(visitor);
    }

    @Test
    void runAllQueries()
    {
        Map<QueryBlock, String> map = musicCurator.runAllQueries();
        // We expected only (and all) queries in "queries-blank" to have new output:
        softly.assertThat(map.size()).isEqualTo(3);
    }

    @Test
    void writeDocument()
            throws IOException
    {
        var original = musicCurator.vault().document("queries-blank").orElseThrow();
        var expected = musicCurator.vault().document("queries-expected").orElseThrow();
        musicCurator.runOnce();
        var update = musicCurator.reload(original);
        assertThat(update).isEqualTo(expected.content());
    }
}

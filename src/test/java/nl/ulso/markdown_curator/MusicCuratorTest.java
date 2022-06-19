package nl.ulso.markdown_curator;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Map;

import static nl.ulso.markdown_curator.vault.ElementCounter.countAll;
import static nl.ulso.markdown_curator.vault.QueryBlockTest.emptyQueryBlock;
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
        musicCurator.runOnce();
    }

    @Test
    void statistics()
    {
        var vault = musicCurator.vault();
        assertThat(vault.name()).endsWith("music");
        var statistics = countAll(vault);
        softly.assertThat(statistics.vaults()).isEqualTo(1);
        softly.assertThat(statistics.folders()).isEqualTo(3);
        softly.assertThat(statistics.documents()).isEqualTo(13);
        softly.assertThat(statistics.frontMatters()).isEqualTo(13);
        softly.assertThat(statistics.sections()).isEqualTo(39);
        softly.assertThat(statistics.queries()).isEqualTo(16);
        softly.assertThat(statistics.codeBlocks()).isEqualTo(5);
        softly.assertThat(statistics.texts()).isEqualTo(56);
    }

    @Test
    void queryCatalog()
    {
        QueryCatalog catalog = musicCurator.queryCatalog();
        softly.assertThat(catalog.queries().size()).isEqualTo(10);
        Query dummy = catalog.query("dummy");
        QueryResult result = dummy.run(emptyQueryBlock());
        var markdown = result.toMarkdown();
        softly.assertThat(markdown).contains("albums");
        softly.assertThat(markdown).contains("recordings");
        softly.assertThat(markdown).contains("members");
    }

    @Test
    void queries()
    {
        var queries = musicCurator.vault().findAllQueryBlocks();
        softly.assertThat(queries.size()).isEqualTo(16);
    }

    @Test
    void internalObjectReferences()
    {
        var visitor = new BreadthFirstVaultVisitor()
        {
            private Folder currentFolder;
            private Document currentDocument;
            private Section currentSection;

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
                currentSection = null;
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
                var tempSection = currentSection;
                currentSection = section;
                super.visit(section);
                currentSection = tempSection;
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
                var section = fragment.parentSection().orElse(null);
                softly.assertThat(section).isEqualTo(currentSection);
            }
        };
        musicCurator.vault().accept(visitor);
    }

    @Test
    void runAllQueries()
    {
        Map<QueryBlock, String> map = musicCurator.runAllQueries();
        // We expect only (and all) queries in "queries-blank" to have new output:
        var list = map.keySet().stream()
                .map(block -> block.document().name())
                .filter(name -> !name.contentEquals("queries-blank"))
                .toList();
        softly.assertThat(list).isEmpty();
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

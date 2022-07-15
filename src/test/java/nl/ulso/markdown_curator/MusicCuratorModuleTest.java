package nl.ulso.markdown_curator;

import com.google.inject.Injector;
import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.event.VaultChangedEvent;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static com.google.inject.Guice.createInjector;
import static nl.ulso.markdown_curator.vault.ElementCounter.countAll;
import static nl.ulso.markdown_curator.vault.QueryBlockTest.emptyQueryBlock;
import static nl.ulso.markdown_curator.vault.event.VaultChangedEvent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SoftAssertionsExtension.class)
class MusicCuratorModuleTest
{
    private Injector injector;
    private Curator musicCurator;
    private DocumentPathResolver documentPathResolver;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeEach
    void constructSystem()
    {
        injector = createInjector(new MusicCuratorModule());
        musicCurator = injector.getInstance(Curator.class);
        documentPathResolver = injector.getInstance(DocumentPathResolver.class);
    }

    @Test
    void statistics()
    {
        var vault = injector.getInstance(Vault.class);
        assertThat(vault.name()).endsWith("music");
        var statistics = countAll(vault);
        softly.assertThat(statistics.vaults()).isEqualTo(1);
        softly.assertThat(statistics.folders()).isEqualTo(3);
        softly.assertThat(statistics.documents()).isEqualTo(13);
        softly.assertThat(statistics.frontMatters()).isEqualTo(13);
        softly.assertThat(statistics.sections()).isEqualTo(37);
        softly.assertThat(statistics.queries()).isEqualTo(14);
        softly.assertThat(statistics.codeBlocks()).isEqualTo(5);
        softly.assertThat(statistics.texts()).isEqualTo(52);
    }

    @Test
    void queryCatalog()
    {
        QueryCatalog catalog = injector.getInstance(QueryCatalog.class);
        softly.assertThat(catalog.queries().size()).isEqualTo(9);
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
        var queries = injector.getInstance(Vault.class).findAllQueryBlocks();
        softly.assertThat(queries.size()).isEqualTo(14);
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
        injector.getInstance(Vault.class).accept(visitor);
    }

    @Test
    void runAllQueries()
    {
        musicCurator.vaultChanged(vaultRefreshed());
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
        var vault = injector.getInstance(Vault.class);
        var original = vault.document("queries-blank").orElseThrow();
        var expected = vault.document("queries-expected").orElseThrow();
        musicCurator.runOnce();
        var update = Files.readString(documentPathResolver.resolveAbsolutePath(original)).trim();
        assertThat(update).isEqualTo(expected.content());
    }
}

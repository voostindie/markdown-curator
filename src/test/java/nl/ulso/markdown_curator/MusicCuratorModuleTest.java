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
import java.nio.file.Files;

import static nl.ulso.markdown_curator.Change.create;
import static nl.ulso.markdown_curator.Changelog.changelogFor;
import static nl.ulso.markdown_curator.vault.ElementCounter.countAll;
import static nl.ulso.markdown_curator.vault.QueryBlockTest.emptyQueryBlock;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class MusicCuratorModuleTest
{
    private Curator musicCurator;
    private Vault vault;
    private QueryCatalog queryCatalog;
    private QueryOrchestratorImpl queryOrchestrator;
    private DocumentPathResolver documentPathResolver;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeEach
    void constructSystem()
    {
        var component = DaggerMusicCurator.create();
        musicCurator = component.curator();
        queryCatalog = component.queryCatalog();
        queryOrchestrator = (QueryOrchestratorImpl) component.queryOrchestrator();
        vault = component.vault();
        documentPathResolver = component.documentPathResolver();
    }

    @Test
    void statistics()
    {
        assertThat(vault.name()).endsWith("music");
        var statistics = countAll(vault);
        softly.assertThat(statistics.vaults()).isEqualTo(1);
        softly.assertThat(statistics.folders()).isEqualTo(4);
        softly.assertThat(statistics.documents()).isEqualTo(14);
        softly.assertThat(statistics.frontMatters()).isEqualTo(14);
        softly.assertThat(statistics.sections()).isEqualTo(38);
        softly.assertThat(statistics.queries()).isEqualTo(14);
        softly.assertThat(statistics.codeBlocks()).isEqualTo(5);
        softly.assertThat(statistics.texts()).isEqualTo(54);
    }

    @Test
    void queryCatalog()
    {
        softly.assertThat(queryCatalog.queries().size()).isEqualTo(15);
        Query dummy = queryCatalog.query("dummy");
        QueryResult result = dummy.run(emptyQueryBlock());
        var markdown = result.toMarkdown();
        softly.assertThat(markdown).contains("albums");
        softly.assertThat(markdown).contains("recordings");
        softly.assertThat(markdown).contains("members");
    }

    @Test
    void queries()
    {
        var queries = vault.findAllQueryBlocks();
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
        vault.accept(visitor);
    }

    @Test
    void runAllQueries()
    {
        var changelog = changelogFor(create(vault, Vault.class));
        var updates = queryOrchestrator.runFor(changelog).stream()
            .map(item -> item.document().name())
            .filter(name -> !name.contentEquals("queries-expected"))
            .toList();
        assertThat(updates).containsExactly("queries-blank");
    }

    @Test
    void writeDocument()
        throws IOException
    {
        var original = vault.document("queries-blank").orElseThrow();
        var expected = vault.document("queries-expected").orElseThrow();
        musicCurator.runOnce();
        var expectedContent = Files.readString(documentPathResolver.resolveAbsolutePath(expected));
        var updatedContent = Files.readString(documentPathResolver.resolveAbsolutePath(original));
        assertThat(updatedContent).isEqualTo(expectedContent);
    }
}

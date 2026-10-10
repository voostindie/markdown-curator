package nl.ulso.curator.query;

import nl.ulso.curator.vault.DocumentPathResolver;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ListFolderContentQueryTest
{
    private final DocumentPathResolver pathResolver = document -> Path.of("/vault", document.name() + ".md");

    @Test
    void name()
    {
        var query = new ListFolderContentQuery(new VaultStub(), pathResolver);
        assertThat(query.name()).isEqualTo("ls");
    }

    @Test
    void description()
    {
        var query = new ListFolderContentQuery(new VaultStub(), pathResolver);
        assertThat(query.description()).isEqualTo("Lists folder content");
    }

    @Test
    void supportedConfiguration()
    {
        var query = new ListFolderContentQuery(new VaultStub(), pathResolver);
        assertThat(query.supportedConfiguration()).containsOnlyKeys("path");
    }

    @Test
    void isImpactedBy()
    {
        var query = new ListFolderContentQuery(new VaultStub(), pathResolver);
        assertThat(query.isImpactedBy(null, null)).isFalse();
    }

    @Test
    void runDefaultPathReturnsVaultRoot()
    {
        var vault = testVault();
        var query = new ListFolderContentQuery(vault, pathResolver);
        var definition = new QueryDefinitionStub(query, vault.resolveDocumentInPath("folder1/doc1"));
        var result = query.run(definition);
        assertThat(result).isInstanceOf(FolderResult.class);
        var folderResult = (FolderResult) result;
        assertThat(folderResult.folder()).isSameAs(vault);
        assertThat(folderResult.documentPathResolver()).isSameAs(pathResolver);
    }

    @Test
    void runSpecificValidPath()
    {
        var vault = testVault();
        var query = new ListFolderContentQuery(vault, pathResolver);
        var definition = new QueryDefinitionStub(query, vault.resolveDocumentInPath("folder1/doc1"))
            .withConfiguration("path", "folder1/subfolder2");
        var result = query.run(definition);
        assertThat(result).isInstanceOf(FolderResult.class);
        var folderResult = (FolderResult) result;
        assertThat(folderResult.folder().name()).isEqualTo("subfolder2");
    }

    @Test
    void runPathWithLeadingTrailingAndExtraSlashes()
    {
        var vault = testVault();
        var query = new ListFolderContentQuery(vault, pathResolver);
        var definition = new QueryDefinitionStub(query, vault.resolveDocumentInPath("folder1/doc1"))
            .withConfiguration("path", "///folder1//subfolder2///");
        var result = query.run(definition);
        assertThat(result).isInstanceOf(FolderResult.class);
        var folderResult = (FolderResult) result;
        assertThat(folderResult.folder().name()).isEqualTo("subfolder2");
    }

    @Test
    void runPathWithInvalidTopLevelFolder()
    {
        var vault = testVault();
        var query = new ListFolderContentQuery(vault, pathResolver);
        var definition = new QueryDefinitionStub(query, vault.resolveDocumentInPath("folder1/doc1"))
            .withConfiguration("path", "nonexistent");
        var result = query.run(definition);
        assertThat(result).isInstanceOf(ErrorResult.class);
        var errorResult = (ErrorResult) result;
        assertThat(errorResult.errorMessage()).isEqualTo("Invalid path: nonexistent.");
    }

    @Test
    void runPathWithInvalidSubfolder()
    {
        var vault = testVault();
        var query = new ListFolderContentQuery(vault, pathResolver);
        var definition = new QueryDefinitionStub(query, vault.resolveDocumentInPath("folder1/doc1"))
            .withConfiguration("path", "folder1/nonexistent");
        var result = query.run(definition);
        assertThat(result).isInstanceOf(ErrorResult.class);
        var errorResult = (ErrorResult) result;
        assertThat(errorResult.errorMessage()).isEqualTo("Invalid path: folder1/nonexistent.");
    }

    private VaultStub testVault()
    {
        var vault = new VaultStub();
        vault.addDocumentInPath("folder1/doc1", "Doc 1 content");
        vault.addDocumentInPath("folder1/subfolder2/doc2", "Doc 2 content");
        return vault;
    }
}

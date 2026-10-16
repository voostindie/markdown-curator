package nl.ulso.curator.query;

import nl.ulso.curator.vault.DocumentPathResolver;
import nl.ulso.curator.vault.FolderStub;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static nl.ulso.curator.query.OutputFormat.JSON;
import static org.assertj.core.api.Assertions.assertThat;

class FolderResultJsonFormatterTest
{
    private final FolderResultJsonFormatter formatter = new FolderResultJsonFormatter();
    private final DocumentPathResolver pathResolver =
        document -> Path.of("/vault", document.name() + ".md");

    @Test
    void queryResultType()
    {
        assertThat(formatter.queryResultType()).isEqualTo(FolderResult.class);
    }

    @Test
    void outputFormat()
    {
        assertThat(formatter.outputFormat()).isEqualTo(JSON);
    }

    @Test
    void formatEmptyFolder()
    {
        var folder = new FolderStub(null, "root");
        var result = new FolderResult(folder, pathResolver);
        var json = formatter.format(result);
        assertThat(json).isEqualTo("{\"documents\":[],\"folders\":[]}");
    }

    @Test
    void formatFolderWithSubfoldersOnly()
    {
        var folder = new FolderStub(null, "root");
        folder.addFolder("Subfolder1");
        var result = new FolderResult(folder, pathResolver);
        var json = formatter.format(result);
        assertThat(json).isEqualTo("{\"documents\":[],\"folders\":[\"Subfolder1\"]}");
    }

    @Test
    void formatFolderWithDocumentsOnlyNoFrontMatter()
    {
        var folder = new FolderStub(null, "root");
        folder.addDocument("Doc1", "# Content");
        var result = new FolderResult(folder, pathResolver);
        var json = formatter.format(result);
        assertThat(json).isEqualTo("""
            {"documents":[{"name":"Doc1","path":"/vault/Doc1.md","last_modified":0,"sortable_title":"Content","properties":{}}],"folders":[]}""");
    }

    @Test
    void formatFolderWithDocumentsAndFrontMatterProperties()
    {
        var folder = new FolderStub(null, "root");
        folder.addDocument("Doc1", """
            ---
            title: Document Title
            rating: 5
            link: "[[Target Page]]"
            tags:
              - tag1
              - tag2
            active: true
            ---
            # Content
            """
        );
        var result = new FolderResult(folder, pathResolver);
        var json = formatter.format(result);
        assertThat(json).isEqualTo("""
            {"documents":[{"name":"Doc1","path":"/vault/Doc1.md","last_modified":0,"sortable_title":"Content","properties":{"active":true,"link":"Target Page","rating":5,"tags":["tag1","tag2"],"title":"Document Title"}}],"folders":[]}""");
    }

    @Test
    void formatFolderWithBothDocumentsAndFolders()
    {
        var folder = new FolderStub(null, "root");
        folder.addFolder("Sub1");
        folder.addDocument("Doc1", "# Just content");
        var result = new FolderResult(folder, pathResolver);
        var json = formatter.format(result);
        assertThat(json).isEqualTo("""
            {"documents":[{"name":"Doc1","path":"/vault/Doc1.md","last_modified":0,\
            "sortable_title":"Just content","properties":{}}],"folders":["Sub1"]}""");
    }
}

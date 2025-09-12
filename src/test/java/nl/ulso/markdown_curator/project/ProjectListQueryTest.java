package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Locale;

import static nl.ulso.markdown_curator.project.ProjectTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ProjectListQueryTest
{
    private VaultStub vault;
    private ProjectListQuery query;

    @BeforeEach
    void setUp()
    {
        vault = createTestVault();
        var projectPropertyRepository = creoateProjectPropertyRepository(vault);
        query = new ProjectListQuery(projectPropertyRepository,
                new ResourceBundledGeneralMessages(Locale.ENGLISH),
                new QueryResultFactory());
    }

    @Test
    void unsupportedFormat()
    {
        var definition = new QueryDefinitionStub(query, vault.resolveDocumentInPath("README"))
                .withConfiguration("format", "foo");
        var result = query.run(definition).toMarkdown();
        assertThat(result).contains("Unsupported format");
    }


    @Test
    void projectTable()
    {
        var definition = new QueryDefinitionStub(query, vault.resolveDocumentInPath("README"))
                .withConfiguration("format", "table");
        var result = query.run(definition).toMarkdown();
        assertThat(result).isEqualTo("""
                
                | Prio | Project       | Lead        | Last&nbsp;modified | Status |
                | ---- | ------------- | ----------- | ------------------ | ------ |
                | 1    | [[Project 3]] | [[Vincent]] | -                  | ⚪️     |
                | -    | [[Project 1]] | [[Vincent]] | [[2025-05-03]]     | 🟢     |
                | -    | [[Project 2]] | [[Marieke]] | -                  | In progress |
                
                """);
    }

    @Test
    void projectList()
    {
        var definition = new QueryDefinitionStub(query, vault.resolveDocumentInPath("README"))
                .withConfiguration("format", "list");
        var result = query.run(definition).toMarkdown();
        assertThat(result).isEqualTo("""
                - [[Project 3]]
                - [[Project 1]]
                - [[Project 2]]
                """);
    }
}

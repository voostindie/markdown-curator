package nl.ulso.curator.addon.project;

import nl.ulso.curator.query.QueryDefinitionStub;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.util.Locale.ENGLISH;
import static nl.ulso.curator.addon.project.ProjectTestData.createAttributeRegistry;
import static nl.ulso.curator.addon.project.ProjectTestData.createProjectRepository;
import static nl.ulso.curator.addon.project.ProjectTestData.createTestVault;
import static nl.ulso.curator.query.QueryModuleTest.createMessages;
import static nl.ulso.curator.query.QueryModuleTest.createQueryResultFactory;
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
        query = new ProjectListQuery(
            createProjectRepository(vault),
            createAttributeRegistry(vault),
            createMessages(ENGLISH),
            createQueryResultFactory()
        );
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

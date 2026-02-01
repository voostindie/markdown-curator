package nl.ulso.curator.addon.project;

import nl.ulso.curator.query.QueryDefinitionStub;
import nl.ulso.curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.util.Locale.ENGLISH;
import static nl.ulso.curator.addon.project.ProjectTestData.createAttributeRegistry;
import static nl.ulso.curator.addon.project.ProjectTestData.createTestVault;
import static nl.ulso.curator.query.QueryModuleTest.createMessages;
import static nl.ulso.curator.query.QueryModuleTest.createQueryResultFactory;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class ProjectLeadQueryTest
{
    private VaultStub vault;
    private ProjectLeadQuery query;

    @BeforeEach
    void setUp()
    {
        vault = createTestVault();
        query = new ProjectLeadQuery(
            createAttributeRegistry(vault),
            createMessages(ENGLISH),
            createQueryResultFactory()
        );
    }

    @Test
    void unknownLead()
    {
        var definition =
            new QueryDefinitionStub(query, vault.resolveDocumentInPath("Projects/Project 1"))
                .withConfiguration("lead", "Yaël");
        var result = query.run(definition).toMarkdown();
        assertThat(result).isEqualTo("No results");
    }

    @Test
    void leadWithOneProject()
    {
        var definition =
            new QueryDefinitionStub(query, vault.resolveDocumentInPath("Contacts/Marieke"));
        var result = query.run(definition).toMarkdown();
        assertThat(result).isEqualTo("""
            
            | Prio | Project       | Last&nbsp;modified | Status |
            | ---- | ------------- | ------------------ | ------ |
            | -    | [[Project 2]] | -                  | In progress |
            
            """);
    }

    @Test
    void leadWithTwoProjects()
    {
        var definition =
            new QueryDefinitionStub(query, vault.resolveDocumentInPath("Contacts/Vincent"));
        var result = query.run(definition).toMarkdown();
        assertThat(result).isEqualTo("""
            
            | Prio | Project       | Last&nbsp;modified | Status |
            | ---- | ------------- | ------------------ | ------ |
            | 1    | [[Project 3]] | -                  | -      |
            | -    | [[Project 1]] | [[2025-05-03]]     | 🟢     |
            
            """);
    }
}

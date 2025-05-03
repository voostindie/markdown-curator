package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.query.*;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Locale;

import static nl.ulso.markdown_curator.project.ProjectTestData.createRepository;
import static nl.ulso.markdown_curator.project.ProjectTestData.createTestAttributeValueResolverRegistry;
import static nl.ulso.markdown_curator.project.ProjectTestData.createTestVault;
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
        var registry = createTestAttributeValueResolverRegistry(vault);
        var repository = createRepository(vault, registry);
        query = new ProjectLeadQuery(repository, new ResourceBundledGeneralMessages(Locale.ENGLISH),
                new QueryResultFactory());
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

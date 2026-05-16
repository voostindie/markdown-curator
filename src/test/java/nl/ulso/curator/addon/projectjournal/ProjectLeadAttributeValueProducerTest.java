package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.vault.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static nl.ulso.curator.change.Change.create;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectLeadAttributeValueProducerTest
{
    private ProjectJournalTestContext testContext;

    @BeforeEach
    void setUp()
    {
        testContext = DaggerProjectJournalTestContext.create();
        createTestDocuments();
        testContext.changeProcessorOrchestrator()
            .runFor(create(testContext.vaultStub(), Vault.class));
    }

    @Test
    void unchangedContext()
    {
        var project = testContext.projectRepository().projectNamed("Project 1").orElseThrow();
        var lead = testContext.projectAttributeRepository()
            .valueOf(project, "lead")
            .orElseThrow();
        assertThat(lead).isEqualTo(testContext.vaultStub().findDocument("Vincent").orElseThrow());
    }

    private void createTestDocuments()
    {
        var vault = testContext.vaultStub();
        vault.addDocumentInPath("Contacts/Vincent", "That's me!");
        vault.addDocumentInPath("Projects/Project 1", "");
        vault.addDocumentInPath("Journal/Markers/Stakeholder", """
            ---
            project-leads:
              - Lead
            ---
            """
        );
        vault.addDocumentInPath("Journal/2026-05-16", """
            ## Activities
            
            - [[Project 1]]
                - [[Vincent]] is the [[Stakeholder|Lead]] of this project.
            """
        );
    }
}

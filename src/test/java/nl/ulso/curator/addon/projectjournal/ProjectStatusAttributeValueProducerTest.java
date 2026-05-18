package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.vault.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nl.ulso.curator.change.Change.create;
import static org.assertj.core.api.Assertions.assertThat;

class ProjectStatusAttributeValueProducerTest
{
    private ProjectJournalTestContext testContext;

    @BeforeEach
    void setUp()
    {
        testContext = DaggerProjectJournalTestContext.create();
        createTestDocuments();
        testContext.changeProcessorOrchestrator()
            .runFor(List.of(create(testContext.vaultStub(), Vault.class)));
    }

    @Test
    void unchangedContext()
    {
        var project = testContext.projectRepository().projectNamed("Project 1").orElseThrow();
        var status = testContext.projectAttributeRepository()
            .valueOf(project, "status")
            .orElseThrow();
        assertThat(status).isEqualTo("In progress");
    }

    private void createTestDocuments()
    {
        var vault = testContext.vaultStub();
        vault.addDocumentInPath("Projects/Project 1", "");
        vault.addDocumentInPath("Journal/Markers/Status", """
            ---
            project-statuses:
              - New
              - In progress
              - Done
            ---
            """
        );
        vault.addDocumentInPath("Journal/2026-05-16", """
            ## Activities
            
            - [[Project 1]]
                - It is still well [[Status|In progress]].
            """
        );
    }
}

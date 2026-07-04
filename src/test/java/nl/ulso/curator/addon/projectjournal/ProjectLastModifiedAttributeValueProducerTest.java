package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.addon.project.ProjectAttributeValue;
import nl.ulso.curator.change.Change;
import nl.ulso.curator.vault.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
import static nl.ulso.curator.change.Change.create;
import static nl.ulso.curator.change.Change.delete;
import static nl.ulso.curator.change.Change.update;
import static nl.ulso.curator.change.ChangeCollector.newChangeCollector;
import static nl.ulso.curator.change.Reset.RESET;
import static org.assertj.core.api.Assertions.assertThat;

/// This is an extensive test case for the last modification attribute alone. The reason is that it
/// also tests the [ProjectJournalAttributeValueProducer] completely, simplifying other test cases.
class ProjectLastModifiedAttributeValueProducerTest
{
    private ProjectJournalTestContext testContext;

    @BeforeEach
    void setUp()
    {
        testContext = DaggerProjectJournalTestContext.create();
        createTestDocuments();
        testContext.changeProcessorOrchestrator().runFor(List.of(RESET));
    }

    @Test
    void unchangedContext()
    {
        testChanges(emptyList(), "Project 1", LocalDate.of(2026, 5, 16));
    }

    @Test
    void deletingMostRecentResultsInPenUltimateDate()
    {
        testChanges(
            List.of(
                delete(
                    testContext.vaultStub().findDocument("2026-05-16").orElseThrow(),
                    Document.class
                )
            ),
            "Project 1",
            LocalDate.of(2026, 5, 15)
        );
    }

    @Test
    void deletingAllReferencesResultsInNoDate()
    {
        testChanges(
            List.of(
                delete(
                    testContext.vaultStub().findDocument("2026-05-16").orElseThrow(),
                    Document.class
                ),
                delete(
                    testContext.vaultStub().findDocument("2026-05-15").orElseThrow(),
                    Document.class
                )
            ),
            "Project 1",
            null
        );
    }

    @Test
    void newJournalReferenceUpdatesDate()
    {
        testChanges(
            List.of(
                create(
                    testContext.vaultStub().addDocumentInPath("Journal/2026-05-17", """
                        ## Activities
                        
                        - Yet another [[Project 1]] reference.
                        """
                    ),
                    Document.class
                )
            ),
            "Project 1",
            LocalDate.of(2026, 5, 17)
        );
    }

    @Test
    void newJournalReferenceInThePastDoesNothing()
    {
        testChanges(
            List.of(
                create(
                    testContext.vaultStub().addDocumentInPath("Journal/2025-01-01", """
                        ## Activities
                        
                        - A very old [[Project 1]] reference.
                        """
                    ),
                    Document.class
                )
            ),
            "Project 1",
            LocalDate.of(2026, 5, 16)
        );
    }

    @Test
    void creatingNewProjectDiscoversExistingDate()
    {
        testChanges(
            List.of(
                create(
                    testContext.vaultStub().addDocumentInPath("Projects/Project 2", ""),
                    Document.class
                )
            ),
            "Project 2",
            LocalDate.of(2026, 5, 15)
        );
    }

    @Test
    void deletingProjectReferenceRevertsToOlderDate()
    {
        testChanges(
            List.of(
                update(
                    testContext.vaultStub().findDocument("2026-05-16").orElseThrow(),
                    testContext.vaultStub().addDocumentInPath("Journal/2026-05-16", ""),
                    Document.class
                )
            ),
            "Project 1",
            LocalDate.of(2026, 5, 15)
        );
    }

    @Test
    void addingProjectReferenceDiscoversNewDate()
    {
        testChanges(
            List.of(
                create(
                    testContext.vaultStub().addDocumentInPath("Projects/Project 2", ""),
                    Document.class
                ),
                update(
                    testContext.vaultStub().findDocument("2026-05-16").orElseThrow(),
                    testContext.vaultStub().addDocumentInPath("Journal/2026-05-16", """
                        ## Activities
                        
                        - Second reference to [[Project 1]].
                        - Second reference to [[Project 2]].
                        """
                    ),
                    Document.class
                )
            ),
            "Project 2",
            LocalDate.of(2026, 5, 16)
        );
    }

    @Test
    void reload()
    {
        var collector = newChangeCollector();
        testContext.projectAttributeLastModifiedValueProducer().reload(collector);
        var changelog = collector.changelog().changes().toList();
        var project = testContext.projectRepository().projectNamed("Project 1").orElseThrow();
        var lastModified = testContext.projectAttributeRepository().attributeDefinitions().stream()
            .filter(definition ->
                definition.frontMatterProperty().contentEquals("last_modified"))
            .findFirst()
            .orElseThrow();
        assertThat(changelog).containsExactly(
            delete(
                new ProjectAttributeValue(
                    project,
                    lastModified,
                    LocalDate.of(2026, 5, 16),
                    100
                ),
                ProjectAttributeValue.class
            ),
            create(
                new ProjectAttributeValue(
                    project,
                    lastModified,
                    LocalDate.of(2026, 5, 16),
                    100
                ),
                ProjectAttributeValue.class
            )
        );
    }

    void testChanges(List<Change<?>> changes, String projectName, LocalDate expectedDate)
    {
        testContext.changeProcessorOrchestrator().runFor(changes);
        var project = testContext.projectRepository().projectNamed(projectName).orElseThrow();
        var optionalDate = testContext.projectAttributeRepository()
            .valueOf(project, "last_modified");
        if (expectedDate == null)
        {
            assertThat(optionalDate).isEmpty();
        }
        else
        {
            var date = optionalDate.orElseThrow();
            assertThat(date).isEqualTo(expectedDate);
        }
    }

    private void createTestDocuments()
    {
        var vault = testContext.vaultStub();
        vault.addDocumentInPath("Projects/Project 1", "");
        vault.addDocumentInPath("Journal/2026-05-15", """
            ## Activities
            
            - Reference to non-existent [[Project 2]].
            - [[Project 1]] reference.
            """
        );
        vault.addDocumentInPath("Journal/2026-05-16", """
            ## Activities
            
            - Second reference to [[Project 1]].
            """
        );
    }
}

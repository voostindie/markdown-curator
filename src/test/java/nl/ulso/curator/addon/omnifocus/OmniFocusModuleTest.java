package nl.ulso.curator.addon.omnifocus;

import nl.ulso.curator.query.QueryDefinitionStub;
import nl.ulso.curator.vault.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static nl.ulso.curator.change.Change.create;
import static org.assertj.core.api.Assertions.assertThat;

/// This is more of a system context than a unit test: it sets up the change processor chain with
/// the project and OmniFocus modules, runs a vault with 3 projects, and an extract from OmniFocus
/// with 3 projects through it, and checks that all the outcomes are as expected.
public class OmniFocusModuleTest
{
    private OmniFocusTestContext testContext;

    @BeforeEach
    void setUp()
    {
        JavaScriptForAutomationStub.setExpectedResult(List.of(
            Map.of(
                "id", "id_1",
                "name", "Project 1",
                "status", "active status"
            ),
            Map.of(
                "id", "id_2",
                "name", "Project 2",
                "status", "active status"
            ),
            Map.of(
                "id", "id_4",
                "name", "Project 4",
                "status", "dropped status"
            )
        ));
        testContext = DaggerOmniFocusTestContext.create();
        var vault = testContext.vaultStub();
        vault.addDocumentInPath("Projects/Project 1", "Project also in OmniFocus");
        vault.addDocumentInPath("Projects/Project 3", "Project not in OmniFocus");
        testContext.changeProcessorOrchestrator()
            .runFor(List.of(create(testContext.vaultStub(), Vault.class)));
    }

    @Test
    void omniFocusProjects()
    {
        var repository = testContext.omniFocusRepository();
        assertThat(repository.projects()).hasSize(2);
    }

    @Test
    void omniFocusUrl()
    {
        var project = testContext.projectRepository().projectNamed("Project 1").orElseThrow();
        var repository = testContext.projectAttributeRepository();
        var url = repository.valueOf(project, "omnifocus").orElseThrow();
        assertThat(url).isEqualTo("omnifocus:///task/id_1");
    }

    @Test
    void priority()
    {
        var project = testContext.projectRepository().projectNamed("Project 1").orElseThrow();
        var repository = testContext.projectAttributeRepository();
        var url = repository.valueOf(project, "priority").orElseThrow();
        assertThat(url).isEqualTo(1);
    }

    @Test
    void queryFindsTwoWayDifferences()
    {
        var query = testContext.omniFocusQuery();
        var markdown = query.run(new QueryDefinitionStub(query, null)).toMarkdown();
        assertThat(markdown).isEqualTo("""
            ### Projects without a matching document
            
            - [[Project 2]]
            
            ### Documents without a matching project
            
            - [[Project 3]] - [(🆕 Create project in OmniFocus)](omnifocus:///paste?index=1&target=/folder/Area&content=Project%203%3A)
            """.trim());
    }
}

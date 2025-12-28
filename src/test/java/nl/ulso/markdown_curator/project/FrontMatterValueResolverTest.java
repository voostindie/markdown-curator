package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.Document;
import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static nl.ulso.markdown_curator.project.ProjectProperty.*;
import static nl.ulso.markdown_curator.project.ProjectProperty.newProperty;
import static nl.ulso.markdown_curator.project.ProjectTestData.PROJECT_PROPERTIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(SoftAssertionsExtension.class)
class FrontMatterValueResolverTest
{
    private VaultStub vault;
    private ProjectPropertyRepository repository;
    private Project project;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        vault.addDocument("Lead", "");
        vault.addDocumentInPath("Projects/Project", """
                ---
                priority: 42
                status: "In progress"
                last_modified: 2025-05-03
                lead: "[[Lead]]"
                ---
                Project description
                """);
        repository = ProjectTestData.creoateProjectPropertyRepository(vault);
        project = repository.projectFor(vault.resolveDocumentInPath("Projects/Project"))
                .orElseThrow();
    }

    @AfterEach
    void tearDown()
    {
        vault = null;
        project = null;
    }

    @Test
    void resolvePriority()
    {
        var priority = repository.propertyValue(project, PROJECT_PROPERTIES.get(PRIORITY))
                .map(i -> (Integer) i);
        assertThat(priority).hasValue(42);
    }

    @Test
    void resolveStatus()
    {
        var status = repository.propertyValue(project, PROJECT_PROPERTIES.get(STATUS))
                .map(s -> (String) s);
        assertThat(status).hasValue("In progress");
    }

    @Test
    void resolveLastModified()
    {
        var lastModified =
                repository.propertyValue(project, PROJECT_PROPERTIES.get(LAST_MODIFIED))
                        .map(d -> (LocalDate) d);
        assertThat(lastModified).hasValue(LocalDate.of(2025, 5, 3));
    }

    @Test
    void resolveLead()
    {
        var lead = repository.propertyValue(project, PROJECT_PROPERTIES.get(LEAD))
                .map(d -> (Document) d);
        assertThat(lead).hasValue(vault.resolveDocumentInPath("Lead"));
    }

    @Test
    void unsupportedType()
    {
        var specialProperty = newProperty(Double.class, "special");
        var resolver = new FrontMatterValueResolver(specialProperty, vault);
        project = repository.projectFor(vault.resolveDocumentInPath("Projects/Project"))
            .orElseThrow();
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> resolver.from(project));
    }
}

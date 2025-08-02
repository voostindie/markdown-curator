package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.VaultStub;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Set;

import static nl.ulso.markdown_curator.project.Attribute.LAST_MODIFIED;
import static nl.ulso.markdown_curator.project.Attribute.LEAD;
import static nl.ulso.markdown_curator.project.Attribute.PRIORITY;
import static nl.ulso.markdown_curator.project.Attribute.STATUS;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SoftAssertionsExtension.class)
class FrontMatterAttributeValueResolverTest
{
    private VaultStub vault;
    private AttributeValueResolverRegistry registry;
    private Project project;

    @BeforeEach
    void setUp()
    {
        vault = new VaultStub();
        vault.addDocument("Lead", "");
        vault.addDocument("Project", """
                ---
                priority: 42
                status: "In progress"
                last-modified: 2025-05-03
                lead: "[[Lead]]"
                ---
                Project description
                """);
        registry = new DefaultAttributeValueResolverRegistry(Set.of(
                new FrontMatterAttributeValueResolver<>(PRIORITY, "priority", vault),
                new FrontMatterAttributeValueResolver<>(STATUS, "status", vault),
                new FrontMatterAttributeValueResolver<>(LAST_MODIFIED, "last-modified",
                        vault),
                new FrontMatterAttributeValueResolver<>(LEAD, "lead", vault)));
        project = new Project(vault.resolveDocumentInPath("Project"), registry);
    }

    @AfterEach
    void tearDown()
    {
        vault = null;
        registry = null;
        project = null;
    }

    @Test
    void resolvePriority()
    {
        var priority = project.attributeValue(PRIORITY);
        assertThat(priority).hasValue(42);
    }

    @Test
    void resolveStatus()
    {
        var status = project.attributeValue(STATUS);
        assertThat(status).hasValue("In progress");
    }

    @Test
    void resolveLastModified()
    {
        var lastModified = project.attributeValue(LAST_MODIFIED);
        assertThat(lastModified).hasValue(LocalDate.of(2025, 5, 3));
    }

    @Test
    void resolveLead()
    {
        var lead = project.attributeValue(LEAD);
        assertThat(lead).hasValue(vault.resolveDocumentInPath("Lead"));
    }
}

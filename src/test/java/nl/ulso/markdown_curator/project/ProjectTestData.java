package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.InMemoryFrontMatterCollector;
import nl.ulso.markdown_curator.vault.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static nl.ulso.markdown_curator.project.ProjectProperty.*;

public class ProjectTestData
{
    static VaultStub createTestVault()
    {
        var vault = new VaultStub();
        vault.addDocumentInPath("README", "");
        vault.addDocumentInPath("Contacts/Vincent", "");
        vault.addDocumentInPath("Contacts/Marieke", "");
        vault.addDocumentInPath("Projects/Project 1", """
                ---
                lead: "[[Vincent]]"
                status: 🟢
                last_modified: 2025-05-03
                ---
                """);
        vault.addDocumentInPath("Projects/Project 2", """
                ---
                lead: "[[Marieke]]"
                status: "In progress"
                ---
                """);
        vault.addDocumentInPath("Projects/Project 3", """
                ---
                lead: "[[Vincent]]"
                priority: 1
                ---
                """);
        return vault;
    }

    static Map<String, ProjectProperty> PROJECT_PROPERTIES = Map.of(
            STATUS, newProperty(String.class, "status"),
            LEAD, newProperty(Document.class, "lead", d -> ((Document) d).link()),
            LAST_MODIFIED,
            newProperty(LocalDate.class, "last_modified", Object::toString),
            PRIORITY, newProperty(Integer.class, "priority")
    );

    static ProjectPropertyRepository creoateProjectPropertyRepository(Vault vault)
    {
        var projectRepository = new ProjectRepository(vault, new ProjectSettings("Projects"));
        projectRepository.fullRefresh();
        var registry = new ProjectPropertyResolverRegistryImpl(Set.of(
                new FrontMatterProjectPropertyResolver(PROJECT_PROPERTIES.get(STATUS), vault),
                new FrontMatterProjectPropertyResolver(PROJECT_PROPERTIES.get(LEAD), vault),
                new FrontMatterProjectPropertyResolver(PROJECT_PROPERTIES.get(LAST_MODIFIED),
                        vault),
                new FrontMatterProjectPropertyResolver(PROJECT_PROPERTIES.get(PRIORITY), vault)
        ));
        var result = new ProjectPropertyRepository(PROJECT_PROPERTIES, projectRepository, registry,
                new InMemoryFrontMatterCollector(vault));
        result.fullRefresh();
        return result;
    }
}

package nl.ulso.curator.addon.project;

import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.VaultStub;

import java.time.LocalDate;
import java.util.Map;

import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.*;
import static nl.ulso.date.LocalDates.parseDateOrNull;

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
            """
        );
        vault.addDocumentInPath("Projects/Project 2", """
            ---
            lead: "[[Marieke]]"
            status: "In progress"
            ---
            """
        );
        vault.addDocumentInPath("Projects/Project 3", """
            ---
            lead: "[[Vincent]]"
            priority: 1
            ---
            """
        );
        return vault;
    }

    public static final Map<String, ProjectAttributeDefinition> ATTRIBUTE_DEFINITIONS = Map.of(
        STATUS, newAttributeDefinition(String.class, STATUS),
        LEAD, newAttributeDefinition(Document.class, LEAD, d -> ((Document) d).link()),
        LAST_MODIFIED, newAttributeDefinition(LocalDate.class, LAST_MODIFIED, Object::toString),
        PRIORITY, newAttributeDefinition(Integer.class, PRIORITY)
    );

    static ProjectAttributeRepositoryStub createAttributeRegistry(VaultStub vault)
    {
        var vincent = vault.resolveDocumentInPath("Contacts/Vincent");
        var marieke = vault.resolveDocumentInPath("Contacts/Marieke");
        var project1 = vault.resolveDocumentInPath("Projects/Project 1");
        var project2 = vault.resolveDocumentInPath("Projects/Project 2");
        var project3 = vault.resolveDocumentInPath("Projects/Project 3");
        return new ProjectAttributeRepositoryStub()
            .withAttribute(project1, "lead", vincent)
            .withAttribute(project1, "status", "🟢")
            .withAttribute(project1, "last_modified", parseDateOrNull("2025-05-03"))
            .withAttribute(project2, "lead", marieke)
            .withAttribute(project2, "status", "In progress")
            .withAttribute(project3, "lead", vincent)
            .withAttribute(project3, "priority", 1);
    }

    static ProjectRepository createProjectRepository(VaultStub vault)
    {
        var project1 = vault.resolveDocumentInPath("Projects/Project 1");
        var project2 = vault.resolveDocumentInPath("Projects/Project 2");
        var project3 = vault.resolveDocumentInPath("Projects/Project 3");
        return new ProjectRepositoryStub()
            .withProject(project1)
            .withProject(project2)
            .withProject(project3);
    }

}

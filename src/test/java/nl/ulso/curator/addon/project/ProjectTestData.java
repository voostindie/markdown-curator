package nl.ulso.curator.addon.project;

import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.VaultStub;

import java.time.LocalDate;
import java.util.*;

import static nl.ulso.curator.addon.project.AttributeDefinition.*;
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

    static final Map<String, AttributeDefinition> ATTRIBUTE_DEFINITIONS = Map.of(
        STATUS, newAttributeDefinition(String.class, STATUS),
        LEAD, newAttributeDefinition(Document.class, LEAD, d -> ((Document) d).link()),
        LAST_MODIFIED, newAttributeDefinition(LocalDate.class, LAST_MODIFIED, Object::toString),
        PRIORITY, newAttributeDefinition(Integer.class, PRIORITY)
    );

    static AttributeRegistryStub createAttributeRegistry(VaultStub vault)
    {
        var vincent = vault.resolveDocumentInPath("Contacts/Vincent");
        var marieke = vault.resolveDocumentInPath("Contacts/Marieke");
        var project1 = vault.resolveDocumentInPath("Projects/Project 1");
        var project2 = vault.resolveDocumentInPath("Projects/Project 2");
        var project3 = vault.resolveDocumentInPath("Projects/Project 3");
        return new AttributeRegistryStub()
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

    static class ProjectRepositoryStub
        implements ProjectRepository
    {
        private final Map<String, Project> projects = new HashMap<>();

        public ProjectRepositoryStub withProject(Document document)
        {
            projects.put(document.name(), new Project(document));
            return this;
        }

        @Override
        public Map<String, Project> projectsByName()
        {
            return projects;
        }

        @Override
        public Collection<Project> projects()
        {
            return projects.values();
        }

        @Override
        public Optional<Project> projectFor(Document document)
        {
            return Optional.ofNullable(projects.get(document.name()));
        }

    }

    static final class AttributeRegistryStub
        implements AttributeRegistry
    {
        private final Map<Project, Map<AttributeDefinition, Object>> attributes = new HashMap<>();

        AttributeRegistryStub withAttribute(Document document, String attributeName, Object value)
        {
            attributes.computeIfAbsent(new Project(document), _ -> new HashMap<>())
                .put(ATTRIBUTE_DEFINITIONS.get(attributeName), value);
            return this;
        }

        @Override
        public Collection<AttributeDefinition> attributeDefinitions()
        {
            return ATTRIBUTE_DEFINITIONS.values();
        }

        @Override
        public Optional<?> valueOf(Project project, String attributeName)
        {
            return valueOf(project, ATTRIBUTE_DEFINITIONS.get(attributeName));
        }

        @Override
        public Optional<?> valueOf(Project project, AttributeDefinition definition)
        {
            return Optional.ofNullable(attributes.get(project).get(definition));
        }
    }
}

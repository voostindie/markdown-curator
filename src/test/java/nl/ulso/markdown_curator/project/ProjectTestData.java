package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.*;

import java.time.LocalDate;
import java.util.*;

import static nl.ulso.markdown_curator.project.AttributeDefinition.*;
import static nl.ulso.markdown_curator.vault.LocalDates.parseDateOrNull;

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

    static AttributeRegistry createAttributeRegistry(VaultStub vault)
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

    private static final class AttributeRegistryStub
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
        public Set<Project> projects()
        {
            return attributes.keySet();
        }

        @Override
        public Collection<AttributeDefinition> attributeDefinitions()
        {
            return ATTRIBUTE_DEFINITIONS.values();
        }

        @Override
        public Optional<?> attributeValue(Project project, String attributeName)
        {
            return attributeValue(project, ATTRIBUTE_DEFINITIONS.get(attributeName));
        }

        @Override
        public Optional<?> attributeValue(Project project, AttributeDefinition definition)
        {
            return Optional.ofNullable(attributes.get(project).get(definition));
        }

        @Override
        public Optional<Project> projectFor(Document document)
        {
            return Optional.empty();
        }
    }

}

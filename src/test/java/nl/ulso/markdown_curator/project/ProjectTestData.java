package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.Vault;
import nl.ulso.markdown_curator.vault.VaultStub;

import java.util.Set;

import static nl.ulso.markdown_curator.project.Attribute.LAST_MODIFIED;
import static nl.ulso.markdown_curator.project.Attribute.LEAD;
import static nl.ulso.markdown_curator.project.Attribute.PRIORITY;
import static nl.ulso.markdown_curator.project.Attribute.STATUS;

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
                last-modified: 2025-05-03
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

    static AttributeValueResolverRegistry createTestAttributeValueResolverRegistry(Vault vault)
    {
        return new DefaultAttributeValueResolverRegistry(Set.of(
                new FrontMatterAttributeValueResolver<>(LAST_MODIFIED, "last-modified", vault),
                new FrontMatterAttributeValueResolver<>(LEAD, "lead", vault),
                new FrontMatterAttributeValueResolver<>(PRIORITY, "priority", vault),
                new FrontMatterAttributeValueResolver<>(STATUS, "status", vault)
        ));
    }

    static ProjectRepository createRepository(Vault vault, AttributeValueResolverRegistry registry)
    {
        var repository =
                new ProjectRepository(vault, () -> registry, new ProjectSettings("Projects"));
        repository.fullRefresh();
        return repository;
    }
}

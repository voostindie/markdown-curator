package nl.ulso.markdown_curator.project;

import nl.ulso.markdown_curator.vault.Document;

import java.util.*;

public interface AttributeRegistry
{
    Set<Project> projects();

    Collection<AttributeDefinition> attributeDefinitions();

    Optional<?> attributeValue(Project project, String attributeName);

    Optional<?> attributeValue(Project project, AttributeDefinition definition);

    Optional<Project> projectFor(Document document);
}

package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.ChangeCollector;
import nl.ulso.curator.change.EntityProcessor;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.Vault;
import nl.ulso.dictionary.Dictionary;

import java.time.LocalDate;
import java.util.*;

/// Produces attribute values for all available attribute definitions from front matter properties.
///
/// Is the default, built-in mechanism for resolving attribute values. All values set by this
/// producer have a weight of 0 and are expected to be overruled by other attribute producers that
/// use larger weights.
@Singleton
public final class FrontMatterProjectAttributeValueProducer
    extends EntityProcessor<Project>
{
    private static final int WEIGHT = 0;
    private final Collection<ProjectAttributeDefinition> projectAttributeDefinitions;
    private final Vault vault;

    @Inject
    FrontMatterProjectAttributeValueProducer(
        Map<String, ProjectAttributeDefinition> attributeDefinitions, Vault vault)
    {
        this.projectAttributeDefinitions = attributeDefinitions.values();
        this.vault = vault;
    }

    @Override
    protected Class<Project> entityClass()
    {
        return Project.class;
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(ProjectAttributeValue.class);
    }

    /// Creates a project attribute value for each supported front matter property in the project
    /// document.
    @Override
    protected void entityCreated(Project newProject, ChangeCollector collector)
    {
        var frontMatter = newProject.document().frontMatter();
        for (var definition : projectAttributeDefinitions)
        {
            convertProperty(definition, frontMatter).ifPresent(value ->
                collector.create(
                    new ProjectAttributeValue(newProject, definition, value, WEIGHT),
                    ProjectAttributeValue.class
                )
            );
        }
    }

    /// Compares the supported front matter properties in the old and new project documents and
    /// creates, updates, or deletes project attribute values accordingly.
    @Override
    protected void entityUpdated(Project oldProject, Project newProject, ChangeCollector collector)
    {
        var oldFrontMatter = oldProject.document().frontMatter();
        var newFrontMatter = newProject.document().frontMatter();
        for (var definition : projectAttributeDefinitions)
        {
            var oldValue = convertProperty(definition, oldFrontMatter);
            var newValue = convertProperty(definition, newFrontMatter);
            if (oldValue.isEmpty() && newValue.isPresent())
            {
                collector.create(
                    new ProjectAttributeValue(newProject, definition, newValue.get(), WEIGHT),
                    ProjectAttributeValue.class
                );
            }
            else if (oldValue.isPresent() && newValue.isEmpty())
            {
                collector.delete(
                    new ProjectAttributeValue(oldProject, definition, null, WEIGHT),
                    ProjectAttributeValue.class
                );
            }
            else if (oldValue.isPresent() && // newValue.isPresent() &&
                     !oldValue.get().equals(newValue.get()))
            {
                collector.update(
                    new ProjectAttributeValue(oldProject, definition, oldValue.get(), WEIGHT),
                    new ProjectAttributeValue(newProject, definition, newValue.get(), WEIGHT),
                    ProjectAttributeValue.class
                );
            }
        }
    }

    /// Deletes all supported project attribute values from front matter properties associated with
    /// the given project.
    @Override
    protected void entityDeleted(Project oldProject, ChangeCollector collector)
    {
        var frontMatter = oldProject.document().frontMatter();
        for (var definition : projectAttributeDefinitions)
        {
            convertProperty(definition, frontMatter).ifPresent(value ->
                collector.delete(
                    new ProjectAttributeValue(oldProject, definition, value, WEIGHT),
                    ProjectAttributeValue.class
                )
            );
        }
    }

    private Optional<?> convertProperty(
        ProjectAttributeDefinition definition, Dictionary frontMatter)
    {
        var frontMatterProperty = definition.frontMatterProperty();
        var valueType = definition.valueType();
        if (valueType.isAssignableFrom(String.class))
        {
            return fromString(frontMatterProperty, frontMatter);
        }
        else if (valueType.isAssignableFrom(Document.class))
        {
            return fromDocument(frontMatterProperty, frontMatter);
        }
        else if (valueType.isAssignableFrom(Integer.class))
        {
            return fromInteger(frontMatterProperty, frontMatter);
        }
        else if (valueType.isAssignableFrom(LocalDate.class))
        {
            return fromLocalDate(frontMatterProperty, frontMatter);
        }
        throw new UnsupportedOperationException(
            String.format("Unsupported front matter type: %s", valueType));
    }

    private Optional<?> fromString(String frontMatterProperty, Dictionary frontMatter)
    {
        return Optional.ofNullable(frontMatter.string(frontMatterProperty, null));
    }

    private Optional<?> fromDocument(String frontMatterProperty, Dictionary frontMatter)
    {
        var result = frontMatter.string(frontMatterProperty, null);
        if (result == null)
        {
            return Optional.empty();
        }
        if (result.startsWith("[[") && result.endsWith("]]"))
        {
            result = result.substring(2, result.length() - 2);
        }
        return vault.findDocument(result);
    }

    private Optional<?> fromLocalDate(String frontMatterProperty, Dictionary frontMatter)
    {
        return Optional.ofNullable(frontMatter.date(frontMatterProperty, null));
    }

    private Optional<?> fromInteger(String frontMatterProperty, Dictionary frontMatter)
    {
        if (!frontMatter.hasProperty(frontMatterProperty))
        {
            return Optional.empty();
        }
        var result = frontMatter.integer(frontMatterProperty, 0);
        return Optional.of(result);
    }

    @Override
    public String name()
    {
        return FrontMatterProjectAttributeValueProducer.class.getSimpleName();
    }
}

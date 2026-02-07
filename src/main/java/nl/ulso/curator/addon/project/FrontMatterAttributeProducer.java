package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;
import nl.ulso.curator.vault.Document;
import nl.ulso.curator.vault.Vault;
import nl.ulso.dictionary.Dictionary;

import java.time.LocalDate;
import java.util.*;

import static nl.ulso.curator.change.Change.*;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

/// Produces attribute values for all available attribute definitions from front matter properties.
///
/// Is the default, built-in mechanism for resolving attribute values. All values set by this
/// producer have a weight of 0 and are expected to be overruled by other attribute producers that
/// use larger weights.
///
/// This processor triggers only on project creation and project updates. There's no need to trigger
/// on project deletes, as in that case all associated attribute values are deleted by the
/// [DefaultAttributeRegistry].
@Singleton
public final class FrontMatterAttributeProducer
    extends ChangeProcessorTemplate
{
    private static final int WEIGHT = 0;
    private final Collection<AttributeDefinition> attributeDefinitions;
    private final Vault vault;

    @Inject
    FrontMatterAttributeProducer(Map<String, AttributeDefinition> attributeDefinitions, Vault vault)
    {
        this.attributeDefinitions = attributeDefinitions.values();
        this.vault = vault;
    }

    @Override
    protected Set<? extends ChangeHandler> createChangeHandlers()
    {
        return Set.of(
            newChangeHandler(isCreate(), this::projectCreated),
            newChangeHandler(isUpdate(), this::projectUpdated)
        );
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Project.class);
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(AttributeValue.class);
    }

    @Override
    protected boolean isResetRequired(Changelog changelog)
    {
        return false;
    }

    private Collection<Change<?>> projectCreated(Change<?> change)
    {
        var project = change.as(Project.class).value();
        var frontMatter = project.document().frontMatter();
        var changes = createChangeCollection();
        for (var definition : attributeDefinitions)
        {
            convertProperty(definition, frontMatter).ifPresent(value ->
                changes.add(create(
                        new AttributeValue(project, definition, value, WEIGHT),
                        AttributeValue.class
                    )
                )
            );
        }
        return changes;
    }

    private Collection<Change<?>> projectUpdated(Change<?> change)
    {
        var oldProject = change.as(Project.class).oldValue();
        var oldFrontMatter = oldProject.document().frontMatter();
        var newProject = change.as(Project.class).newValue();
        var newFrontMatter = newProject.document().frontMatter();
        var changes = createChangeCollection();
        for (var definition : attributeDefinitions)
        {
            var oldValue = convertProperty(definition, oldFrontMatter);
            var newValue = convertProperty(definition, newFrontMatter);
            if (oldValue.isEmpty() && newValue.isPresent())
            {
                changes.add(create(
                        new AttributeValue(newProject, definition, newValue.get(), WEIGHT),
                        AttributeValue.class
                    )
                );
            }
            else if (oldValue.isPresent() && newValue.isEmpty())
            {
                changes.add(delete(
                        new AttributeValue(oldProject, definition, null, WEIGHT),
                        AttributeValue.class
                    )
                );
            }
            else if (oldValue.isPresent()) // && newValue.isPresent()
            {
                if (!oldValue.get().equals(newValue.get()))
                {
                    changes.add(update(
                            new AttributeValue(oldProject, definition, oldValue.get(), WEIGHT),
                            new AttributeValue(newProject, definition, newValue.get(), WEIGHT),
                            AttributeValue.class
                        )
                    );
                }
            }
        }
        return changes;
    }

    private Optional<?> convertProperty(
        AttributeDefinition definition, Dictionary frontMatter)
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
}

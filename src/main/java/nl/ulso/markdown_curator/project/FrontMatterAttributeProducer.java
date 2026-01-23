package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.*;
import nl.ulso.markdown_curator.vault.*;
import nl.ulso.markdown_curator.vault.Dictionary;

import java.time.LocalDate;
import java.util.*;

import static nl.ulso.markdown_curator.Change.Kind.DELETE;
import static nl.ulso.markdown_curator.Change.create;
import static nl.ulso.markdown_curator.Change.delete;

/// Produces attribute values for all available attribute definitions from front matter properties.
///
/// Is the default, built-in mechanism for resolving attribute values. All values set by this
/// producer have a weight of 0 and are expected to be overruled by other attribute producers.
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
        registerChangeHandler(_ -> true, this::processProject);
    }

    @Override
    public Set<Class<?>> consumedObjectTypes()
    {
        return Set.of(Project.class);
    }

    @Override
    public Set<Class<?>> producedObjectTypes()
    {
        return Set.of(AttributeValue.class);
    }

    @Override
    protected boolean isFullRefreshRequired(Changelog changelog)
    {
        return false;
    }

    private Collection<Change<?>> processProject(Change<?> change)
    {
        var project = change.as(Project.class).object();
        var document = project.document();
        var changes = createChangeCollection();
        for (var definition : attributeDefinitions)
        {
            if (change.kind() == DELETE)
            {
                changes.add(delete(
                    new AttributeValue(project, definition, null, WEIGHT),
                    AttributeValue.class
                ));
            }
            else
            {
                var frontMatterProperty = definition.frontMatterProperty();
                var frontMatter = document.frontMatter();
                convertProperty(frontMatterProperty, frontMatter, definition).ifPresentOrElse(
                    value ->
                        changes.add(create(
                                new AttributeValue(
                                    project,
                                    definition,
                                    value,
                                    WEIGHT
                                ),
                                AttributeValue.class
                            )
                        ),
                    () ->
                        changes.add(delete(
                                new AttributeValue(
                                    project,
                                    definition,
                                    null,
                                    WEIGHT
                                ),
                                AttributeValue.class
                            )
                        )
                );
            }
        }
        return changes;
    }

    private Optional<?> convertProperty(
        String frontMatterProperty, Dictionary frontMatter, AttributeDefinition definition)
    {
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
        var result = frontMatter.integer(frontMatterProperty, Integer.MIN_VALUE);
        if (result == Integer.MIN_VALUE)
        {
            return Optional.empty();
        }
        return Optional.of(result);
    }
}

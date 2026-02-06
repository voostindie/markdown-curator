package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;
import nl.ulso.curator.main.*;

import java.util.Collection;
import java.util.Set;

import static java.util.Collections.emptyList;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

/// Writes project attribute values to front matter.
///
/// This processor consumes the [AttributeRegistryUpdate] payload type to ensure it will be executed
/// *after* the [AttributeRegistry] is fully updated. That means this processor can safely resolve
/// attribute values through the registry for all projects.
///
/// Note that the attribute value that is part of the [AttributeValue] that is consumed might not be
/// the final and correct value for the attribute. For example, the update might have been
/// irrelevant because there is another value with a higher weight that did not change.
@Singleton
public final class FrontMatterPropertyWriter
    extends ChangeProcessorTemplate
{
    private final AttributeRegistry attributeRegistry;
    private final FrontMatterCollector frontMatterCollector;

    @Inject
    FrontMatterPropertyWriter(
        AttributeRegistry attributeRegistry,
        FrontMatterCollector frontMatterCollector)
    {
        this.attributeRegistry = attributeRegistry;
        this.frontMatterCollector = frontMatterCollector;
    }

    @Override
    protected Set<? extends ChangeHandler> createChangeHandlers()
    {
        return Set.of(
            newChangeHandler(
                isPayloadType(AttributeValue.class),
                this::processAttributeValue
            )
        );
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(AttributeRegistryUpdate.class, AttributeValue.class);
    }

    @Override
    protected boolean isResetRequired(Changelog changelog)
    {
        return false;
    }

    private Collection<Change<?>> processAttributeValue(Change<?> change)
    {
        var attributeValue = change.as(AttributeValue.class).value();
        var project = attributeValue.project();
        var definition = attributeValue.definition();
        frontMatterCollector.updateFrontMatterFor(project.document(), dictionary ->
            attributeRegistry.attributeValue(project, definition).ifPresentOrElse(
                value ->
                    dictionary.setProperty(
                        definition.frontMatterProperty(),
                        definition.asFrontMatterValue(value)
                    ),
                () -> dictionary.removeProperty(definition.frontMatterProperty())
            )
        );
        return emptyList();
    }
}

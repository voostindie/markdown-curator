package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.ChangeProcessorTemplate;
import nl.ulso.curator.changelog.Change;
import nl.ulso.curator.changelog.Changelog;
import nl.ulso.curator.main.*;

import java.util.Collection;
import java.util.Set;

import static java.util.Collections.emptyList;
import static nl.ulso.curator.changelog.Change.isPayloadType;

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
    private final FrontMatterUpdateCollector frontMatterUpdateCollector;

    @Inject
    FrontMatterPropertyWriter(
        AttributeRegistry attributeRegistry,
        FrontMatterUpdateCollector frontMatterUpdateCollector)
    {
        this.attributeRegistry = attributeRegistry;
        this.frontMatterUpdateCollector = frontMatterUpdateCollector;
        registerChangeHandler(isPayloadType(AttributeValue.class), this::processAttributeValue);
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(AttributeRegistryUpdate.class, AttributeValue.class);
    }

    @Override
    protected boolean isFullRefreshRequired(Changelog changelog)
    {
        return false;
    }

    private Collection<Change<?>> processAttributeValue(Change<?> change)
    {
        var attributeValue = change.as(AttributeValue.class).value();
        var project = attributeValue.project();
        var definition = attributeValue.definition();
        frontMatterUpdateCollector.updateFrontMatterFor(project.document(), dictionary ->
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

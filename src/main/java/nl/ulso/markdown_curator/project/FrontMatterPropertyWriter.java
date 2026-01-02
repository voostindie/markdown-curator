package nl.ulso.markdown_curator.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.markdown_curator.*;

import java.util.*;

import static java.util.Collections.emptyList;

/// Writes project attribute values to front matter.
///
/// By the time this model gets processed, all attribute values for all projects have been resolved
/// where necessary: the [AttributeRegistryImpl] has been consuming all these changes, building up its
/// internal registry.
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
    }

    @Override
    public Set<Class<?>> consumedObjectTypes()
    {
        return Set.of(AttributeRegistryUpdate.class);
    }

    @Override
    protected boolean isFullRefreshRequired(Changelog changelog)
    {
        return true;
    }

    @Override
    public Collection<Change<?>> fullRefresh()
    {
        attributeRegistry.projects().forEach(project ->
            frontMatterUpdateCollector.updateFrontMatterFor(project.document(), dictionary ->
                attributeRegistry.attributeDefinitions().forEach(definition ->
                    attributeRegistry.attributeValue(project, definition).ifPresentOrElse(
                        value ->
                            dictionary.setProperty(
                                definition.frontMatterProperty(),
                                definition.asFrontMatterValue(value)
                            ),
                        () ->
                            dictionary.removeProperty(definition.frontMatterProperty())
                    ))
            ));
        return emptyList();
    }
}

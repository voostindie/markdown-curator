package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;
import nl.ulso.curator.main.FrontMatterCollector;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;
import static org.slf4j.LoggerFactory.getLogger;

/// Writes project attribute values to front matter.
///
/// This processor consumes the [ProjectAttributeRepositoryUpdate] payload type to ensure it will be
/// executed *after* the [ProjectAttributeRepository] is fully updated. That means this processor
/// can safely resolve attribute values through the registry for all projects.
///
/// Note that the attribute value that is part of the [ProjectAttributeValue] that is consumed might
/// not be the final and correct value for the attribute. For example, the update might have been
/// irrelevant because there is another value with a higher weight that did not change.
@Singleton
public final class ProjectAttributeValueFrontMatterWriter
    extends ChangeProcessorTemplate
{
    private static final Logger LOGGER = getLogger(ProjectAttributeValueFrontMatterWriter.class);

    private final ProjectAttributeRepository projectAttributeRepository;
    private final FrontMatterCollector frontMatterCollector;

    @Inject
    ProjectAttributeValueFrontMatterWriter(
        ProjectAttributeRepository projectAttributeRepository,
        FrontMatterCollector frontMatterCollector)
    {
        this.projectAttributeRepository = projectAttributeRepository;
        this.frontMatterCollector = frontMatterCollector;
    }

    @Override
    protected List<? extends ChangeHandler> createChangeHandlers()
    {
        return List.of(
            newChangeHandler(
                isPayloadType(ProjectAttributeValue.class),
                this::attributeValueChanged
            )
        );
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(ProjectAttributeRepositoryUpdate.class, ProjectAttributeValue.class);
    }

    /// When an attribute value has changed - created, updated, or deleted - that doesn't
    /// necessarily mean anything: it depends on the weight of the attribute value.
    ///
    /// The solution is straightforward: we pull the _actual_ value from the
    /// [ProjectAttributeRepository] and set that in the front matter, or remove it from the front
    /// matter if no such value exists any longer. We then trust on the front matter system to
    /// ignore meaningless updates.
    private void attributeValueChanged(Change<?> change, ChangeCollector collector)
    {
        var attributeValue = change.as(ProjectAttributeValue.class).value();
        var project = attributeValue.project();
        var definition = attributeValue.definition();
        frontMatterCollector.updateFrontMatterFor(project.document(), dictionary ->
            projectAttributeRepository.valueOf(project, definition)
                .ifPresentOrElse(
                    value ->
                    {
                        LOGGER.trace(
                            "Setting front matter property '{}' to '{}' for document '{}'.",
                            definition.frontMatterProperty(),
                            definition.asFrontMatterValue(value),
                            project.document().name()
                        );
                        dictionary.setProperty(
                            definition.frontMatterProperty(),
                            definition.asFrontMatterValue(value)
                        );
                    },
                    () ->
                    {
                        LOGGER.trace(
                            "Removing front matter property '{}' from document '{}'.",
                            definition.frontMatterProperty(),
                            project.document().name()
                        );
                        dictionary.removeProperty(definition.frontMatterProperty());
                    }
                )
        );
    }

    @Override
    public String name()
    {
        return ProjectAttributeValueFrontMatterWriter.class.getSimpleName();
    }
}

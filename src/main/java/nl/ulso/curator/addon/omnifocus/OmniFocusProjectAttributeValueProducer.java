package nl.ulso.curator.addon.omnifocus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.addon.project.*;
import nl.ulso.curator.change.*;
import nl.ulso.curator.statistics.MeasurementCollector;
import nl.ulso.curator.statistics.MeasurementTracker;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static nl.ulso.curator.addon.omnifocus.OmniFocusProject.NULL_PROJECT;
import static nl.ulso.curator.addon.project.ProjectAttributeDefinition.PRIORITY;
import static nl.ulso.curator.change.Change.isDelete;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;

/// Produces attribute values for projects from matching projects in OmniFocus.
///
/// 2 attributes are produced:
///
/// - Priority of the project in OmniFocus
/// - URL to the OmniFocus project
@Singleton
final class OmniFocusProjectAttributeValueProducer
    extends ChangeProcessorTemplate
    implements MeasurementTracker
{
    static final String OMNIFOCUS_URL_ATTRIBUTE = "omnifocus";
    private static final int WEIGHT = 200;

    private final ProjectAttributeDefinition priorityAttribute;
    private final ProjectAttributeDefinition urlAttribute;
    private final ProjectRepository projectRepository;
    private final DefaultOmniFocusRepository omniFocusRepository;
    private final Set<String> knownProjectNames;

    @Inject
    OmniFocusProjectAttributeValueProducer(
        Map<String, ProjectAttributeDefinition> attributeDefinitions,
        ProjectRepository projectRepository,
        DefaultOmniFocusRepository omniFocusRepository)
    {
        this.priorityAttribute = requireNonNull(attributeDefinitions.get(PRIORITY));
        this.urlAttribute = requireNonNull(attributeDefinitions.get(OMNIFOCUS_URL_ATTRIBUTE));
        this.projectRepository = projectRepository;
        this.omniFocusRepository = omniFocusRepository;
        this.knownProjectNames = new HashSet<>();
    }

    @Override
    protected List<? extends ChangeHandler> createChangeHandlers()
    {
        return List.of(
            newChangeHandler(
                isPayloadType(OmniFocusUpdate.class),
                this::omniFocusUpdated
            ),
            newChangeHandler(
                isPayloadType(Project.class).and(isDelete()),
                this::projectDeleted
            )
        );
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(OmniFocusUpdate.class, Project.class);
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(ProjectAttributeValue.class);
    }

    @Override
    protected void reset()
    {
        knownProjectNames.clear();
    }

    private void omniFocusUpdated(Change<?> change, ChangeCollector collector)
    {
        projectRepository.projects().forEach(project ->
            omniFocusRepository.projectNamed(project.name()).ifPresentOrElse(
                omniFocusProject ->
                {
                    if (knownProjectNames.contains(project.name()))
                    {
                        updateProjectAttributes(collector, project, omniFocusProject);
                    }
                    else
                    {
                        createProjectAttributes(collector, project, omniFocusProject);
                    }
                },
                () -> deleteProjectAttributes(project, collector)
            ));
    }

    private void projectDeleted(Change<?> change, ChangeCollector collector)
    {
        var project = change.as(Project.class).value();
        deleteProjectAttributes(project, collector);
    }

    private void createProjectAttributes(
        ChangeCollector collector, Project project, OmniFocusProject omniFocusProject)
    {
        knownProjectNames.add(project.name());
        collector.create(
            priority(project, omniFocusProject),
            ProjectAttributeValue.class
        );
        collector.create(
            url(project, omniFocusProject),
            ProjectAttributeValue.class
        );
    }

    private void updateProjectAttributes(
        ChangeCollector collector, Project project, OmniFocusProject omniFocusProject)
    {
        collector.update(
            priority(project, omniFocusProject),
            ProjectAttributeValue.class
        );
        collector.update(
            url(project, omniFocusProject),
            ProjectAttributeValue.class
        );
    }

    private void deleteProjectAttributes(Project project, ChangeCollector collector)
    {
        if (knownProjectNames.contains(project.name()))
        {
            collector.delete(priority(project, NULL_PROJECT), ProjectAttributeValue.class);
            collector.delete(url(project, NULL_PROJECT), ProjectAttributeValue.class);
            knownProjectNames.remove(project.name());
        }
    }

    private ProjectAttributeValue url(
        Project project, OmniFocusProject omniFocusProject)
    {
        return new ProjectAttributeValue(
            project,
            urlAttribute,
            omniFocusProject.link(),
            WEIGHT
        );
    }

    private ProjectAttributeValue priority(
        Project project, OmniFocusProject omniFocusProject)
    {
        return new ProjectAttributeValue(
            project,
            priorityAttribute,
            omniFocusProject.priority(),
            WEIGHT
        );
    }

    @Override
    public void collectMeasurements(MeasurementCollector collector)
    {
        collector.total(OmniFocusProject.class, knownProjectNames.size());
    }
}

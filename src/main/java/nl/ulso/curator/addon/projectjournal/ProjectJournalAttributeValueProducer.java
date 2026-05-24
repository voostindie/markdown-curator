package nl.ulso.curator.addon.projectjournal;

import nl.ulso.curator.addon.journal.Daily;
import nl.ulso.curator.addon.journal.Journal;
import nl.ulso.curator.addon.project.*;
import nl.ulso.curator.change.*;
import nl.ulso.curator.statistics.MeasurementCollector;
import nl.ulso.curator.statistics.MeasurementTracker;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.reverseOrder;
import static nl.ulso.curator.change.Change.isCreate;
import static nl.ulso.curator.change.Change.isDelete;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.Change.isUpdate;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;
import static org.slf4j.LoggerFactory.getLogger;

/// Base class for change processors that produce values for a specific project attribute definition
/// from daily entries in the journal.
///
/// The implementation ensures that the most recent attribute value that can be extracted from the
/// journal is always used.
///
/// Subclasses need to implement code to extract the actual attribute value from a daily journal
/// entry and little else.
///
/// The implementation keeps track of the most recent attribute value for all projects in the
/// repository. It needs this administration to know what to do when a daily is modified. If the
/// daily is older than the most recent daily, this change can be ignored. If the daily with the
/// most recent attribute value for a project is deleted or modified so that it no longer contains
/// an attribute value, the implementation actively searches backwards through the journal for the
/// (previous) most recent value. This is potentially expensive, especially when there is no such
/// value. In that case the implementation will have visited all dailies, and that number grows over
/// time. Anyhow, this is not common, and the implementation handles it just fine; it just gets a
/// little slower. Since everything happens in memory, it's not a concern.
abstract class ProjectJournalAttributeValueProducer
    extends ChangeProcessorTemplate
    implements MeasurementTracker
{
    private static final Logger LOGGER = getLogger(ProjectJournalAttributeValueProducer.class);
    private static final int WEIGHT = 100;

    private final Journal journal;
    private final ProjectRepository projectRepository;
    private final ProjectAttributeDefinition attributeDefinition;
    private final Map<String, DatedValue> datedValues;

    ProjectJournalAttributeValueProducer(
        ProjectRepository projectRepository,
        Journal journal,
        ProjectAttributeDefinition attributeDefinition)
    {
        this.journal = journal;
        this.projectRepository = projectRepository;
        this.attributeDefinition = attributeDefinition;
        this.datedValues = new HashMap<>();
    }

    @Override
    protected final void reset()
    {
        datedValues.clear();
    }

    @Override
    public Set<Class<?>> consumedPayloadTypes()
    {
        return Set.of(Project.class, Daily.class);
    }

    @Override
    public final Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(ProjectAttributeValue.class);
    }

    /// Forces a complete reload of all attribute values tracked by this producer. This method is
    /// available for subclasses and not actively called by this class itself. Ideally, it is never
    /// called because it is a bit of a waste.
    protected final void reload(ChangeCollector collector)
    {
        LOGGER.trace("Reloading attribute values for all projects.");
        projectRepository.projects().forEach(project ->
            {
                deleteAttributeFor(project, collector);
                updateLatestAttributeValueFor(project, collector);
            }
        );
    }

    @Override
    protected List<? extends ChangeHandler> createChangeHandlers()
    {
        return List.of(
            newChangeHandler(
                isPayloadType(Project.class).and(isCreate()),
                this::projectCreated
            ),
            newChangeHandler(
                isPayloadType(Project.class).and(isDelete()),
                this::projectDeleted
            ),
            newChangeHandler(
                isPayloadType(Daily.class).and(isCreate()),
                this::dailyCreated
            ),
            newChangeHandler(
                isPayloadType(Daily.class).and(isUpdate()),
                this::dailyUpdated
            ),
            newChangeHandler(
                isPayloadType(Daily.class).and(isDelete()),
                this::dailyDeleted
            )
        );
    }

    /// Go through the journal, from latest to oldest, and find the most recent attribute value for
    /// the new project.
    private void projectCreated(Change<?> change, ChangeCollector collector)
    {
        var newProject = change.as(Project.class).newValue();
        LOGGER.trace("Finding all attributes for new project '{}'.", newProject);
        findLatestAttributeValue(newProject).ifPresent(datedValue ->
            updateAttributeFor(newProject, datedValue, collector)
        );
    }

    /// Optionally produce a deletion for its attribute value if there was one.
    private void projectDeleted(Change<?> change, ChangeCollector collector)
    {
        var oldProject = change.as(Project.class).oldValue();
        LOGGER.trace("Removing all attributes for deleted project '{}'.", oldProject);
        deleteAttributeFor(oldProject, collector);
    }

    /// When a daily is created, check for each project if the daily resolves to a more recent
    /// attribute value than is currently being tracked (if any). If so, produce it.
    private void dailyCreated(Change<?> change, ChangeCollector collector)
    {
        var newDaily = change.as(Daily.class).newValue();
        LOGGER.trace("Finding all project attributes for new daily '{}'.", newDaily);
        processProjectAdditionsToDaily(
            projectRepository.projects().stream()
                .filter(project -> newDaily.refersTo(project.name())),
            newDaily,
            collector
        );
    }

    // When a daily is updated, check for each project if it was added to, deleted from, or updated
    // in the daily.
    private void dailyUpdated(Change<?> change, ChangeCollector collector)
    {
        var oldDaily = change.as(Daily.class).oldValue();
        var newDaily = change.as(Daily.class).newValue();
        LOGGER.trace("Updating all project attributes for updated daily '{}'.", newDaily);
        processProjectAdditionsToDaily(
            projectRepository.projects().stream()
                .filter(project -> !oldDaily.refersTo(project.name())
                                   && newDaily.refersTo(project.name())),
            newDaily,
            collector
        );
        processProjectUpdatesInDaily(
            projectRepository.projects().stream()
                .filter(project -> oldDaily.refersTo(project.name())
                                   && newDaily.refersTo(project.name())),
            newDaily,
            collector
        );
        processProjectRemovalsFromDaily(
            projectRepository.projects().stream()
                .filter(project -> oldDaily.refersTo(project.name()) &&
                                   !newDaily.refersTo(project.name())),
            oldDaily,
            collector
        );
    }

    /// When a daily is deleted, for each project that has an attribute value based on it, resolve a
    /// new (less recent) attribute value and produce it.
    private void dailyDeleted(Change<?> change, ChangeCollector collector)
    {
        var oldDaily = change.as(Daily.class).oldValue();
        LOGGER.trace("Updating all project attributes for removed daily '{}'.", oldDaily);
        var projects = projectRepository.projects().stream()
            .filter(project -> oldDaily.refersTo(project.name()));
        processProjectRemovalsFromDaily(projects, oldDaily, collector);
    }

    private void processProjectAdditionsToDaily(
        Stream<Project> selectedProjects,
        Daily daily,
        ChangeCollector collector)
    {
        selectedProjects
            .filter(project -> hasNewerProjectReference(daily, project))
            .forEach(project ->
                resolveAttributeValue(project, daily)
                    .map(value -> new DatedValue(daily.date(), value))
                    .ifPresent(datedValue ->
                        updateAttributeFor(project, datedValue, collector)
                    )
            );
    }

    private void processProjectRemovalsFromDaily(
        Stream<Project> selectedProjects,
        Daily daily,
        ChangeCollector collector)
    {
        selectedProjects
            .filter(project -> hasAttributeValue(daily, project))
            .forEach(project -> updateLatestAttributeValueFor(project, collector));
    }

    private void processProjectUpdatesInDaily(
        Stream<Project> selectedProjects,
        Daily daily,
        ChangeCollector collector)
    {
        selectedProjects
            .filter(project -> hasAttributeValue(daily, project)
                               || hasNewerProjectReference(daily, project))
            .forEach(project ->
                resolveAttributeValue(project, daily).ifPresentOrElse(
                    newValue ->
                    {
                        var newDatedValue = new DatedValue(daily.date(), newValue);
                        updateAttributeFor(project, newDatedValue, collector);
                    },
                    () ->
                    {
                        if (datedValues.containsKey(project.name()))
                        {
                            updateLatestAttributeValueFor(project, collector);
                        }
                    }
                )
            );
    }

    private void updateLatestAttributeValueFor(Project project, ChangeCollector collector)
    {
        findLatestAttributeValue(project).ifPresentOrElse(
            newDatedValue -> updateAttributeFor(project, newDatedValue, collector),
            () -> deleteAttributeFor(project, collector)
        );
    }

    private Optional<DatedValue> findLatestAttributeValue(Project project)
    {
        return journal.dailiesFor(project.name())
            .sorted(reverseOrder())
            .map(daily -> resolveAttributeValue(project, daily)
                .map(value -> new DatedValue(daily.date(), value))
                .orElse(null)
            )
            .filter(Objects::nonNull)
            .findFirst();
    }

    private boolean hasAttributeValue(Daily newDaily, Project project)
    {
        var datedValue = datedValues.get(project.name());
        return datedValue != null && newDaily.date().isEqual(datedValue.date());
    }

    private boolean hasNewerProjectReference(Daily daily, Project project)
    {
        var datedValue = datedValues.get(project.name());
        return datedValue == null || daily.date().isAfter(datedValue.date());
    }

    private void updateAttributeFor(
        Project project, DatedValue newDatedValue, ChangeCollector collector)
    {
        var oldDatedValue = datedValues.put(project.name(), newDatedValue);
        if (oldDatedValue != null)
        {
            if (oldDatedValue.value().equals(newDatedValue.value()))
            {
                LOGGER.trace(
                    "Attribute value for project '{}' is unchanged. No update needed",
                    project
                );
                return;
            }
            collector.update(
                attributeValue(project, oldDatedValue.value()),
                attributeValue(project, newDatedValue.value()),
                ProjectAttributeValue.class
            );
        }
        else
        {
            collector.create(
                attributeValue(project, newDatedValue.value()),
                ProjectAttributeValue.class
            );
        }
    }

    private void deleteAttributeFor(Project project, ChangeCollector collector)
    {
        var oldDatedValue = datedValues.remove(project.name());
        if (oldDatedValue != null)
        {
            collector.delete(
                attributeValue(project, oldDatedValue.value()),
                ProjectAttributeValue.class
            );
        }
    }

    private ProjectAttributeValue attributeValue(Project project, Object value)
    {
        return new ProjectAttributeValue(project, attributeDefinition, value, WEIGHT);
    }

    abstract Optional<Object> resolveAttributeValue(Project project, Daily daily);

    @Override
    public void collectMeasurements(MeasurementCollector collector)
    {
        collector.total(
            "projectjournal",
            attributeDefinition.frontMatterProperty(),
            datedValues.size()
        );
    }

    private record DatedValue(LocalDate date, Object value) {}
}

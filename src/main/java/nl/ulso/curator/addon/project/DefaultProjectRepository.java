package nl.ulso.curator.addon.project;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.ulso.curator.change.*;
import nl.ulso.curator.vault.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static nl.ulso.curator.change.Change.isCreate;
import static nl.ulso.curator.change.Change.isDelete;
import static nl.ulso.curator.change.Change.isPayloadType;
import static nl.ulso.curator.change.Change.isUpdate;
import static nl.ulso.curator.change.ChangeHandler.newChangeHandler;
import static org.slf4j.LoggerFactory.getLogger;

/// Represents a repository of projects on top of a single folder in the vault, excluding its
/// subfolders.
///
/// This repository only keeps track of _active_ projects. A project is considered active if it is
/// in the main project folder. As soon as a project is archived, typically by moving it to a
/// subfolder, the project is no longer considered active and therefore not tracked by this
/// repository.
///
/// For each project that is created, updated, or deleted, this repository publishes a change with
/// the relevant [Project] as its payload.
@Singleton
final class DefaultProjectRepository
    extends ChangeProcessorTemplate
    implements ProjectRepository
{
    private static final Logger LOGGER = getLogger(DefaultProjectRepository.class);

    private final Vault vault;
    private final String projectFolderName;
    private final Map<String, Project> projects;

    @Inject
    DefaultProjectRepository(Vault vault, ProjectSettings settings)
    {
        this.vault = vault;
        this.projectFolderName = settings.projectFolderName();
        this.projects = new HashMap<>();
    }

    @Override
    protected Set<? extends ChangeHandler> createChangeHandlers()
    {
        return Set.of(
            newChangeHandler(isProjectDocument().and(isCreate()), this::createProject),
            newChangeHandler(isProjectDocument().and(isUpdate()), this::updateProject),
            newChangeHandler(isProjectDocument().and(isDelete()), this::deleteProject)
        );
    }

    @Override
    public Set<Class<?>> producedPayloadTypes()
    {
        return Set.of(Project.class);
    }

    @Override
    protected boolean isResetRequired(Changelog changelog)
    {
        return super.isResetRequired(changelog) ||
               changelog.changes().anyMatch(isProjectFolder().and(isDelete().or(isCreate())));
    }

    Predicate<Change<?>> isProjectDocument()
    {
        return isPayloadType(Document.class).and(change ->
            isProjectFolder(change.as(Document.class).value().folder())
        );
    }

    private Predicate<Change<?>> isProjectFolder()
    {
        return isPayloadType(Folder.class).and(change ->
            isProjectFolder(change.as(Folder.class).value()));
    }

    private boolean isProjectFolder(Folder folder)
    {
        return folder != vault &&
               folder.parent() == vault &&
               folder.name().contentEquals(projectFolderName);
    }

    @Override
    public void reset(ChangeCollector collector)
    {
        projects.clear();
        var finder = new ProjectFinder();
        vault.accept(finder);
        finder.projects.forEach(project ->
        {
            projects.put(project.name(), project);
            collector.create(project, Project.class);
        });
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Built a project repository of {} projects", projects.size());
        }
    }

    private void createProject(Change<?> change, ChangeCollector collector)
    {
        var document = change.as(Document.class).value();
        var project = new Project(document);
        projects.put(project.name(), project);
        collector.create(project, Project.class);
    }

    private void updateProject(Change<?> change, ChangeCollector collector)
    {
        var document = change.as(Document.class).value();
        var newProject = new Project(document);
        var oldProject = projects.put(newProject.name(), newProject);
        collector.update(oldProject, newProject, Project.class);
    }

    private void deleteProject(Change<?> change, ChangeCollector collector)
    {
        var document = change.as(Document.class).value();
        var project = projects.remove(document.name());
        collector.delete(project, Project.class);
    }

    @Override
    public Map<String, Project> projectsByName()
    {
        return unmodifiableMap(projects);
    }

    @Override
    public Collection<Project> projects()
    {
        return unmodifiableCollection(projects.values());
    }

    @Override
    public Optional<Project> projectFor(Document document)
    {
        return Optional.ofNullable(projects.get(document.name()));
    }

    private class ProjectFinder
        extends BreadthFirstVaultVisitor
    {
        private final List<Project> projects = new ArrayList<>();

        @Override
        public void visit(Vault vault)
        {
            // Visit only the project folder, not the whole vault, nor the subfolders.
            vault.folder(projectFolderName).ifPresent(
                folder -> folder.documents()
                    .forEach(document -> document.accept(this)));
        }

        @Override
        public void visit(Document document)
        {
            projects.add(new Project(document));
        }
    }
}
